package com.project.shopapp.controllers;

import com.project.shopapp.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("${api.prefix}/report")
@RequiredArgsConstructor
public class ReportController {
    private final OrderService orderService;

    @GetMapping()
    public ResponseEntity<Map<String, Object>> getOrderReport(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Map<String, Object> report = orderService.getOrderReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportOrderReportToExcel(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) throws IOException {

        // Xuất dữ liệu báo cáo thành file Excel
        byte[] excelData = orderService.exportOrderReportToExcel(startDate, endDate);

        // Trả về tệp Excel
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order_report.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
