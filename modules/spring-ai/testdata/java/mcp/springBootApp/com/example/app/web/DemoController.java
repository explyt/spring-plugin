/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.example.app.web;

import com.example.app.service.DemoService;
import com.example.app.dto.DemoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService service;

    public DemoController(DemoService service) {
        this.service = service;
    }

    @GetMapping(value = "/items/{id}", produces = "application/json")
    public DemoDto getItem(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PostMapping(value = "/items", consumes = "application/json", produces = "application/json")
    public DemoDto createItem(@RequestBody DemoDto body) {
        return service.save(body);
    }
}
