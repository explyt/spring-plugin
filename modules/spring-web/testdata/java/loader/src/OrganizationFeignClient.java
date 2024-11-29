package com

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "organizationFeignClient", url = "${service.organization.api.url}")
public interface OrganizationFeignClient {
    @PostMapping(value = "get")
    Object get(@RequestBody Object dto);
}