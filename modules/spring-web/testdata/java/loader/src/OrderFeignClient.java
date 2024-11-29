package com

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "OrderFeignClient", url = "https://market.com",  path = "/v1")
public interface OrderFeignClient {
    @PostMapping(value = "order")
    Object order(@RequestBody Object dto);
}