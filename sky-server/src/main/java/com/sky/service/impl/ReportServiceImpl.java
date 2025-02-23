package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
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
}
