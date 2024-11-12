package com

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "productFeignClient")
public interface ProductFeignClient {
    @PostMapping(value = "product")
    Object product(@RequestBody Object dto);
}