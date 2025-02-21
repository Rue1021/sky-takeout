package com.sky.service;

import com.github.pagehelper.Page;
import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
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

    /**
     * 订单条件搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult orderConditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statusStatistics();

    /**
     * 管理端查询订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void orderConfirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void orderRejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 订单派送
     * @param id
     */
    void orderDelivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);
}
