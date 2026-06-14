/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.app.repository;

import com.example.app.dto.DemoDto;
import org.springframework.stereotype.Repository;

@Repository
public class DemoRepository {

    public DemoDto load(Long id) {
        return new DemoDto(id, "name-" + id);
    }

    public DemoDto store(DemoDto dto) {
        return dto;
    }
}
