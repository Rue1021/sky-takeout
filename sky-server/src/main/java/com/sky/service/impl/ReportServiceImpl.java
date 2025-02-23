package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO statistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = dateList(begin, end);

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTimeForEachDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTimeForEachDay = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("begin", beginTimeForEachDay);
            map.put("end", endTimeForEachDay);
            map.put("status", Orders.COMPLETED);

            Double turnover =  orderMapper.sumByMap(map);
            turnover = (turnover == null ? 0 : turnover);
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .turnoverList(StringUtils.join(turnoverList,','))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = dateList(begin, end);

        List<Integer> totalUserList = new ArrayList<>();

        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTimeForEachDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTimeForEachDay = LocalDateTime.of(date, LocalTime.MAX);

            //查询新用户，限定时间为当天
            Map mapForNew = new HashMap<>();
            mapForNew.put("begin", beginTimeForEachDay);
            mapForNew.put("end", endTimeForEachDay);

            //查询总用户，限定时间为这天之前的所有时间
            Map mapForTotal = new HashMap<>();
            mapForTotal.put("end", endTimeForEachDay);

            Integer totalUser = userMapper.countUserByMap(mapForTotal);
            totalUser = (totalUser == null ? 0 : totalUser);
            totalUserList.add(totalUser);

            Integer newUser = userMapper.countUserByMap(mapForNew);
            newUser = (newUser == null ? 0 : newUser);
            newUserList.add(newUser);

        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .totalUserList(StringUtils.join(totalUserList,','))
                .newUserList(StringUtils.join(newUserList,','))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        OrderReportVO orderReportVO = new OrderReportVO();

        List<LocalDate> dateList = dateList(begin, end);

        orderReportVO.setDateList(StringUtils.join(dateList,","));

        List<Integer> orderCountList = new ArrayList<>();

        List<Integer> validOrderCountList = new ArrayList<>();

        Integer totalOrderCount = null;

        Integer validOrderCount = null;

        for (LocalDate date : dateList) {
            LocalDateTime beginTimeForEachDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTimeForEachDay = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("begin", beginTimeForEachDay);
            map.put("end", endTimeForEachDay);

            //统计每日订单数
            Integer dailyTotalOrder = orderMapper.countOrderByMap(map);
            dailyTotalOrder = (dailyTotalOrder == null ? 0 : dailyTotalOrder);
            orderCountList.add(dailyTotalOrder);

            //把status放进map，再用动态sql查找有效订单数
            map.put("status", Orders.COMPLETED);

            Integer dailyValidOrder = orderMapper.countOrderByMap(map);
            dailyValidOrder = (dailyValidOrder == null ? 0 : dailyValidOrder);
            validOrderCountList.add(dailyValidOrder);

        }

        Map mapForTotal = new HashMap<>();
        mapForTotal.put("end", LocalDateTime.of(end, LocalTime.MAX));

        totalOrderCount = orderMapper.countOrderByMap(mapForTotal);

        mapForTotal.put("status", Orders.COMPLETED);

        validOrderCount = orderMapper.countOrderByMap(mapForTotal);

        orderReportVO.setOrderCountList(StringUtils.join(orderCountList,","));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList,","));
        orderReportVO.setTotalOrderCount(totalOrderCount);
        orderReportVO.setValidOrderCount(validOrderCount);
        Double orderCompleteRate = totalOrderCount == null ? 0.0 : validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        orderReportVO.setOrderCompletionRate(orderCompleteRate);

        return orderReportVO;
    }

    /**
     * 抽象出私有方法，返回一个日期list
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> dateList(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<LocalDate> dateList = new ArrayList<>();

        if (beginTime.isBefore(endTime)) {
            dateList.add(begin);

            while (begin.isBefore(end)) {
                begin = begin.plusDays(1);
                dateList.add(begin);
            }

        }
        return dateList;
    }
}
