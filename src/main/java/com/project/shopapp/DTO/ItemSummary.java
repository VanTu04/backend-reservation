package com.project.shopapp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemSummary {
    private String itemName;
    private BigDecimal unitPrice;
    private int totalQuantity;
    private BigDecimal totalPrice;
}