package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
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
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private WebSocketServer webSocketServer;


    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //购物车为空、地址簿为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = addressBook.getUserId();

        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
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
        //使用时间戳作为订单号，要把Long型转为String
        orders.setNumber(String.valueOf(System.currentTimeMillis()));

        //后面订单明细需要使用到这个订单实体类，所以插入以后需要返回主键值
        orderMapper.insert(orders);//

        //向订单明细表插入数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

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

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //根据订单号修改数据库
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        Integer orderPaidStatus = Orders.PAID;
        Integer orderStatus = Orders.TO_BE_CONFIRMED;
        LocalDateTime checkOutTime = LocalDateTime.now();
        orderMapper.updateStatus(orderStatus, orderPaidStatus, checkOutTime, orderNumber);

        Orders orders = orderMapper.getByOrderNumber(orderNumber);

        //用户支付成功后，通过websocket向客户端浏览器推送消息
        /*
        1. 通过websocket实现管理端页面和服务器保持长连接状态
        2. 客户支付以后，调用websocket相关api实现服务端向客户端推送消息
        3. 客户端浏览器解析服务端推送的消息，判断是来单提醒还是客户催单，进行相应的消息提示和语音播报
         */
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + orderNumber);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

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

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
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
     * 用户查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetailQuery(Long id) {

        Orders orders = orderMapper.getByOrderId(id);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

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
    public void orderCancel(Long id) {

        Orders orders = orderMapper.getByOrderId(id);

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

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult orderConditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单状态，需要额外返回订单菜品信息
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {

        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();

        if(!(CollectionUtils.isEmpty(ordersList))) {
            for (Orders orders : ordersList) {

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                //将订单菜品信息封装到orderVO,并添加到要返回的list集合
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 私有方法，根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {

        List<OrderDetail> list = orderDetailMapper.getByOrderId(orders.getId());

        //将每一条订单菜品信息拼接为字符串
        List<String> orderDishesList = list.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        //将该订单所有菜品信息拼接在一起
        return String.join("", orderDishesList);
    }


    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statusStatistics() {

        //根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmedNumber = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmedNumber = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgressNumber = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        //将统计结果封装进VO对象返回
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmedNumber);
        orderStatisticsVO.setConfirmed(confirmedNumber);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgressNumber);
        return orderStatisticsVO;
    }

    /**
     * 管理端查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {

        Orders orders = orderMapper.getByOrderId(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void orderConfirm(OrdersConfirmDTO ordersConfirmDTO) {

        Orders orders = Orders.builder()
                        .id(ordersConfirmDTO.getId())
                        .status(Orders.CONFIRMED)
                        .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void orderRejection(OrdersRejectionDTO ordersRejectionDTO) {

        Orders orders = orderMapper.getByOrderId(ordersRejectionDTO.getId());

        //订单必须存在且状态为待接单，才可以拒单
        if (orders == null || !(orders.getStatus().equals(Orders.TO_BE_CONFIRMED))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //拒单+退款
        orders.setStatus(Orders.CANCELLED);
        orders.setPayStatus(Orders.REFUND);

        //拒单原因
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        //取消时间
        orders.setCancelTime(LocalDateTime.now());

        //更新数据库订单状态
        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {

        //创建订单退款的基础信息
        Orders order2Cancel = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        //如已经付款 改为退款状态
        Orders ordersDB = orderMapper.getByOrderId(ordersCancelDTO.getId());
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            order2Cancel.setPayStatus(Orders.REFUND);
        }

        //更新数据库订单状态
        orderMapper.update(order2Cancel);
    }

    /**
     * 订单派送
     * @param id
     */
    @Override
    public void orderDelivery(Long id) {

        Orders ordersDB = orderMapper.getByOrderId(id);

        //只有状态为待派送的订单可以执行派送订单操作
        if (ordersDB == null || !(ordersDB.getStatus().equals(Orders.CONFIRMED))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        //更新数据库订单状态
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {

        Orders ordersDB = orderMapper.getByOrderId(id);

        if (ordersDB == null || !(ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //完成订单需要更新送达时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        //更新数据库订单状态
        orderMapper.update(orders);
    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void remind(Long id) {

        String orderNumber = orderMapper.getByOrderId(id).getNumber();

        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content","订单号：" + orderNumber);
        String json = JSON.toJSONString(map);

        //通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(json);
    }
}
