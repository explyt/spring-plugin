/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.app.integration.persistence.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Deliberately long in every dimension a response is measured by: a deep package, a long class name and enough
 * columns that its details alone exceed a small budget.
 *
 * An entity-count limit says nothing about the size of a response; this one is the counter-example that makes
 * a character budget testable.
 */
@Entity
@Table(
        name = "warehouse_inventory_adjustment_records",
        indexes = {
                @Index(name = "ix_warehouse_inventory_adjustment_records_adjusted_at", columnList = "adjusted_at"),
                @Index(name = "ix_warehouse_inventory_adjustment_records_warehouse", columnList = "warehouse_code")
        }
)
public class WarehouseInventoryAdjustmentEntity {

    @Id
    @Column(name = "inventory_adjustment_identifier")
    private Long inventoryAdjustmentIdentifier;

    @Column(name = "warehouse_code", nullable = false)
    private String warehouseCode;

    @Column(name = "stock_keeping_unit_reference", nullable = false)
    private String stockKeepingUnitReference;

    @Column(name = "adjusted_quantity_in_base_units")
    private Long adjustedQuantityInBaseUnits;

    @Column(name = "previous_quantity_in_base_units")
    private Long previousQuantityInBaseUnits;

    @Column(name = "adjustment_reason_code")
    private String adjustmentReasonCode;

    @Column(name = "adjustment_reason_description")
    private String adjustmentReasonDescription;

    @Column(name = "adjusted_by_operator_identifier")
    private String adjustedByOperatorIdentifier;

    @Column(name = "adjusted_at")
    private String adjustedAt;

    @Column(name = "source_document_reference")
    private String sourceDocumentReference;

    @Column(name = "source_system_correlation_identifier")
    private String sourceSystemCorrelationIdentifier;

    @Column(name = "reconciliation_batch_identifier")
    private String reconciliationBatchIdentifier;
}
