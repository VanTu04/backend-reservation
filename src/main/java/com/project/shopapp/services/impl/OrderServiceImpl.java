package com.project.shopapp.services.impl;

import com.project.shopapp.DTO.ItemSummary;
import com.project.shopapp.DTO.OrderDTO;
import com.project.shopapp.DTO.OrderItemDTO;
import com.project.shopapp.DTO.PaymentStatusDTO;
import com.project.shopapp.customexceptions.DataNotFoundException;
import com.project.shopapp.customexceptions.InvalidParamException;
import com.project.shopapp.enums.PAYMENT_STATUS;
import com.project.shopapp.enums.RESERVATION_STATUS;
import com.project.shopapp.models.Order;
import com.project.shopapp.models.OrderItem;
import com.project.shopapp.models.Product;
import com.project.shopapp.models.TableReservation;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.TableReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements com.project.shopapp.services.OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TableReservationRepository tableReservationRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public Order createOrder(OrderDTO orderDTO) throws DataNotFoundException {
        TableReservation tableReservation = tableReservationRepository.findById(orderDTO.getTableReservationId())
                .orElseThrow(() -> new DataNotFoundException("TableReservation not found"));
        if(!tableReservation.getStatus().equals(RESERVATION_STATUS.CONFIRMED)){
            throw new DataNotFoundException("TableReservation must be confirmed");
        }
        Order order = new Order();
        order.setOrderTime(LocalDateTime.now());
        order.setReservation(tableReservation);
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for(OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
            Product food = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new DataNotFoundException("Product not found"));
            OrderItem orderItem = new OrderItem();
            orderItem.setFood(food);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setOrder(order);

            // Tính tổng giá OrderItem rồi cộng vào totalPrice
            totalPrice = totalPrice.add(food.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);
        order.setPaymentStatus(orderDTO.isPaymentStatus() ? PAYMENT_STATUS.PAID : PAYMENT_STATUS.UNPAID);
        tableReservation.setStatus(RESERVATION_STATUS.SUCCESS);
        tableReservationRepository.save(tableReservation);
        return orderRepository.save(order);
    }

    @Override
    public Page<Order> getOrdersByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderTime"));
        return orderRepository.findAll(pageable);
    }

    @Override
    public Order getOrder(Long id) {
        return orderRepository.findById(id).orElse(null);
    }


    @Override
    public void deleteOrder(Long id) {
        orderRepository.findById(id).ifPresent(orderRepository::delete);
    }

    @Override
    public List<Order> findByTableReservationId(Long id) {
        TableReservation tableReservation = tableReservationRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("TableReservation not found"));
        List<Order> orders = orderRepository.findByReservation(tableReservation);
        return orders;
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public Order updateOrder(OrderDTO orderDTO, Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException("Order not found"));
        order.setOrderTime(LocalDateTime.now());

        orderItemRepository.deleteByOrder(order);

        List<OrderItem> updatedOrderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
            Product food = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Food not found"));

            // Tạo `OrderItem` mới
            OrderItem orderItem = new OrderItem();
            orderItem.setFood(food);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setOrder(order);

            // Tính tổng giá cho `OrderItem` mới
            totalPrice = totalPrice.add(food.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            updatedOrderItems.add(orderItem);
        }
        orderItemRepository.saveAll(updatedOrderItems);

        order.setOrderItems(updatedOrderItems);
        order.setTotalPrice(totalPrice);
        order.setPaymentStatus(orderDTO.isPaymentStatus() ? PAYMENT_STATUS.PAID : PAYMENT_STATUS.UNPAID);
        return orderRepository.save(order);
    }

    @Override
    public Order changeStatus(PaymentStatusDTO paymentStatusDTO) throws InvalidParamException {
        Order existingOrder = orderRepository.findById(paymentStatusDTO.getId()).orElseThrow(() -> new DataNotFoundException("Not found order"));
        PAYMENT_STATUS convertedStatus;
        try {
            convertedStatus = PAYMENT_STATUS.valueOf(paymentStatusDTO.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidParamException("Invalid payment status: " + paymentStatusDTO.getStatus());
        }
        if(existingOrder.getPaymentStatus().equals(convertedStatus)){
            throw new InvalidParamException("Payment status has changed");
        }
        existingOrder.setPaymentStatus(convertedStatus);
        return orderRepository.save(existingOrder);
    }

    @Override
    public Map<String, Object> getOrderReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // so luong hoa don
        Long totalOrders = orderRepository.countOrdersByDateRange(startDateTime, endDateTime);
        // so luong don dat ban thanh cong
        Long successTableReservation = tableReservationRepository.countSuccessTableReservationByDateRange(startDateTime, endDateTime);
        Long cancelledTableReservation = tableReservationRepository.countCanceledOrdersByDateRange(startDateTime, endDateTime);
        BigDecimal totalRevenue = orderRepository.sumTotalRevenueByDateRange(startDateTime, endDateTime);
        List<ItemSummary> orders = getListOrderItemByDateRange(startDate, endDate);

        Map<String, Object> report = new HashMap<>();
        report.put("totalOrders", totalOrders);
        report.put("successTableReservation", successTableReservation);
        report.put("cancelledTableReservation", cancelledTableReservation);
        report.put("totalRevenue", totalRevenue);
        report.put("listOrders", orders);

        return report;
    }

    private List<ItemSummary> getListOrderItemByDateRange(LocalDate startDate, LocalDate endDate) {
        // Lấy danh sách OrderItem dựa trên khoảng thời gian của Order
        List<OrderItem> items = orderItemRepository.findItemsByDateRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        // Gộp dữ liệu và sắp xếp
        Map<String, ItemSummary> itemSummaryMap = new HashMap<>();
        for (OrderItem item : items) {
            String itemName = item.getFood().getName();
            BigDecimal unitPrice = item.getFood().getPrice();
            int quantity = item.getQuantity();
            BigDecimal itemTotalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

            if (itemSummaryMap.containsKey(itemName)) {
                ItemSummary summary = itemSummaryMap.get(itemName);
                summary.setTotalQuantity(summary.getTotalQuantity() + quantity);
                summary.setTotalPrice(summary.getTotalPrice().add(itemTotalPrice));
            } else {
                itemSummaryMap.put(itemName, new ItemSummary(itemName, unitPrice, quantity, itemTotalPrice));
            }
        }

        // Chuyển sang danh sách, sắp xếp và xuất ra Excel (như đã làm ở phần trước)
        return itemSummaryMap.values().stream()
                .sorted((a, b) -> b.getTotalPrice().compareTo(a.getTotalPrice())) // Sắp xếp giảm dần theo totalPrice
                .toList();
    }

    @Override
    public byte[] exportOrderReportToExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<ItemSummary> sortedSummaries = getListOrderItemByDateRange(startDate, endDate);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Item Summary Report");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Item Name");
        headerRow.createCell(1).setCellValue("Unit Price (VND)");
        headerRow.createCell(2).setCellValue("Total Quantity");
        headerRow.createCell(3).setCellValue("Total Price (VND)");

        int rowNum = 1;
        for (ItemSummary summary : sortedSummaries) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(summary.getItemName());
            row.createCell(1).setCellValue(summary.getUnitPrice().toString());
            row.createCell(2).setCellValue(summary.getTotalQuantity());
            row.createCell(3).setCellValue(summary.getTotalPrice().toString());
        }

        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }


}
