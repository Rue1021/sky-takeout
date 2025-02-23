package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO statistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间的用户数据
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间的订单数据
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO ordersStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定区间的销量前10菜品
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO topSales(LocalDate begin, LocalDate end);

    /**
     * 运营数据报表导出
     * @param response
     */
    void businessDataExport(HttpServletResponse response);
}
