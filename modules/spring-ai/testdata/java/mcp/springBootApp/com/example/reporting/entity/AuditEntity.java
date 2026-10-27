/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Lives outside `com.example.app`, so a filter on that package can be shown to exclude something that exists
 * rather than to match everything there is.
 */
@Entity
@Table(name = "audit_log")
public class AuditEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "actor")
    private String actor;
}
