package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageQueryForUser(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询历史订单
     * @param id
     * @return
     */
    OrderVO orderDetailQuery(Long id);

    /**
     * 取消订单
     * @param id
     */
    void orderCancel(Long id) throws Exception;

    /**
     * 再来一单
     * @param id
     */
    void orderAgain(Long id);
}
