package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
}
