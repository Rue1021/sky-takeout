package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
     * 销量前十统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO topSales(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> list = orderMapper.topSales(beginTime, endTime);

//        List<String> nameList = new ArrayList<>();
//        List<Integer> numberList = new ArrayList<>();
//
//        for (GoodsSalesDTO goodsSalesDTO : list) {
//            nameList.add(goodsSalesDTO.getName());
//            numberList.add(goodsSalesDTO.getNumber());
//        }
        //TODO 注释掉的代码可以使用stream流简化
        List<String> nameList = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());


        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
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

    /**
     * 运营数据报表导出
     * @param response
     */
    @Override
    public void businessDataExport(HttpServletResponse response) {
        //1. 查询数据库，获取近30天营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDateTime beginTime = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);

        //2. 通过POI将数据写入excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/reportTemplate.xlsx");

        try {
            //基于模版文件创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //填充概览数据
            XSSFSheet sheet = excel.getSheetAt(0); //得到表格文件第一页
            //得到第二行的第二个单元格并写入
            sheet.getRow(1).getCell(1).setCellValue(beginTime + "-" + endTime);
            //同样的找到单元格写入数据
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));
                XSSFRow row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }

            //3. 通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            in.close();
            excel.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }
}
