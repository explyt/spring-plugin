/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A third entity in the same package, so a page smaller than the inventory leaves a remainder to continue with.
 *
 * It declares no `@Table`, which is what proves a table name falls back to the class name rather than going
 * missing.
 */
@Entity
public class ShipmentEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "carrier")
    private String carrier;
}
