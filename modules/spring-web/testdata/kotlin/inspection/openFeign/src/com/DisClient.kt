package com

import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.Objects

@FeignClient(name = "dgis-client")
interface DisClient {
    @LoadBalanced
    @PostMapping(
        path = ["/universal/plan/calculation-async"], consumes = ["application/json"], produces = ["application/json"]
    )
    fun runPlanCalculationAsync(@RequestBody planTask: Objects): Objects
}