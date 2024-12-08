package com.project.shopapp.services;

import com.project.shopapp.DTO.OrderDTO;
import com.project.shopapp.DTO.PaymentStatusDTO;
import com.project.shopapp.customexceptions.DataNotFoundException;
import com.project.shopapp.customexceptions.InvalidParamException;
import com.project.shopapp.models.Order;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderService {
    Page<Order> getOrdersByPage(int page, int size);
    Order createOrder(OrderDTO orderDTO) throws DataNotFoundException;
    Order getOrder(Long id);
    void deleteOrder(Long id);
    List<Order> findByTableReservationId(Long id);
    List<Order> findAll();

    Order updateOrder(@Valid OrderDTO orderDTO, Long id);

    Order changeStatus(PaymentStatusDTO paymentStatusDTO) throws InvalidParamException;

    Map<String, Object> getOrderReport(LocalDate startDate, LocalDate endDate);
    byte[] exportOrderReportToExcel(LocalDate startDate, LocalDate endDate) throws IOException;
}

