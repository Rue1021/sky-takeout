package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 替换微信支付 更新数据库状态
     * @param orderStatus
     * @param orderPaidStatus
     * @param checkOutTime
     * @param orderNumber
     */
    @Update("update orders set status = #{orderStatus}, pay_status = #{orderPaidStatus}, checkout_time = #{checkOutTime} where number = #{orderNumber}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime checkOutTime, String orderNumber);

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单信息
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getByOrderId(Long id);

    /**
     * 返回所有订单
     * @return
     */
    @Select("select * from orders")
    List<Orders> get();

    /**
     * 返回所有订单的状态
     * @return
     */
    @Select("select status from orders")
    List<Integer> getStatus();

    /**
     * 根据状态统计订单数量
     * @param status
     * @return
     */
    @Select(("select count(id) from orders where status = #{status}"))
    Integer countStatus(Integer status);

    /**
     * 查询是否有超时未支付订单/是否有一直处于派送中的订
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLessThan(Integer status, LocalDateTime orderTime);

    /**
     * 根据订单号查找订单
     * @param orderNumber
     * @return
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByOrderNumber(String orderNumber);

    /**
     * 根据动态条件统计订单量
     * @param map
     * @return
     */
    Integer countOrderByMap(Map map);

    /**
     * 根据动态条件统计营业额
     * @param map
     * @return
     */
    Double sumByMap(Map map);



    List<GoodsSalesDTO> topSales(LocalDateTime beginTime, LocalDateTime endTime);
}
