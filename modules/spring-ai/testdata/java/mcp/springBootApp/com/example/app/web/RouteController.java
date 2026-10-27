/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.app.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A literal route and a template route that both match {@code GET /api/routes/export}.
 *
 * Spring dispatches to the literal one, and the find tool has to say so by the order of its matches; a
 * URL under the same prefix that matches neither has these two as its nearest routes.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @GetMapping("/{id}")
    public String byId(@PathVariable("id") String id) {
        return id;
    }

    @GetMapping("/export")
    public String export() {
        return "export";
    }
}
