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

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;

/**
 * A handler whose signature mixes the four annotation-bound sources with a parameter Spring injects
 * itself and one bound by a custom {@code HandlerMethodArgumentResolver}.
 *
 * The last kind is the point of the fixture: it carries no binding annotation, so an enumeration
 * driven purely by annotations drops it, and a dropped parameter is indistinguishable from an absent
 * one — which matters most for exactly the parameter that carries authorization.
 */
@RestController
@RequestMapping("/api/resolver")
public class ResolverController {

    @GetMapping("/items/{id}")
    public String getItem(
            @PathVariable("id") Long id,
            @RequestParam(value = "q", required = false) String query,
            @CookieValue("sid") String sessionId,
            CurrentUser currentUser,
            WebRequest webRequest,
            Locale locale
    ) {
        return id + query + sessionId + currentUser + webRequest + locale;
    }
}
