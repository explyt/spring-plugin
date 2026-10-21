/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Carries the schema facts a compact listing must leave out: declared indexes, a relationship with its owning
 * side, and a column whose name differs from its field.
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "ix_orders_placed_at", columnList = "placed_at"),
                @Index(name = "ux_orders_reference", columnList = "reference", unique = true)
        }
)
public class OrderEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "placed_at")
    private String placedAt;

    @ManyToOne
    @JoinColumn(name = "demo_id")
    private DemoEntity demo;

    @Transient
    private String notPersisted;

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }
}
