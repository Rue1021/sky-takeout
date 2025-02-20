package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;


    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //1. 处理各种业务异常 --购物车为空、地址簿为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = addressBook.getUserId();
        // Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //2. 向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        String orderAddress = addressBook.getProvinceName() +
                    addressBook.getCityName() +
                    addressBook.getDistrictName() +
                    addressBook.getDetail() + "";
        orders.setAddress(orderAddress);
        //订单号 --使用时间戳作为订单号，要把Long型转为String
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        //用户名userName

        //后面订单明细需要使用到这个订单实体类，所以插入以后需要返回主键值
        orderMapper.insert(orders);

        //3. 向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //4. 清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5. 封装VO返回对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(ordersSubmitDTO.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //得到订单号，订单号唯一，根据订单号修改数据库
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        Integer orderPaidStatus = Orders.PAID; //支付状态，已支付
        Integer orderStatus = Orders.TO_BE_CONFIRMED; //订单状态，待接单
        LocalDateTime checkOutTime = LocalDateTime.now(); //给支付时间check_out_time属性赋值

        orderMapper.updateStatus(orderStatus, orderPaidStatus, checkOutTime, orderNumber);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQueryForUser(OrdersPageQueryDTO ordersPageQueryDTO) {

        //设置分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //查订单表
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        //遍历出每一个订单得到订单id，查订单明细表，封装到VO返回
        if(page != null && page.getTotal() > 0) {

            for (Orders orders : page) {
                Long orderId = orders.getId();

                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                list.add(orderVO);
            }
        }


        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetailQuery(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getByOrderId(id);


        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //将该订单及其详情封装到orderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);

        if (orderDetailList != null && orderDetailList.size() > 0) {
            orderVO.setOrderDetailList(orderDetailList);
        }

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    @Transactional
    public void orderCancel(Long id) throws Exception {

        Orders orders = orderMapper.getByOrderId(id);

        //校验订单是否存在
        if(orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        //用户只能取消1 2状态的订单，3 4 5状态下 取消订单需要致电商家
        if(orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //订单处于2状态 取消订单时需要进行退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND);
        }
            //更新订单状态、取消原因、取消时间
            orders.setStatus(Orders.CANCELLED);
            orders.setCancelReason("用户取消");
            orders.setCancelTime(LocalDateTime.now());
            orderMapper.update(orders);

    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void orderAgain(Long id) {

        //根据订单id查找当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        //将详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = new ArrayList<>();

        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);

            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCart.setUserId(orderMapper.getByOrderId(id).getUserId());

            shoppingCartList.add(shoppingCart);
        }

        shoppingCartMapper.insertBatch(shoppingCartList);
    }


}
