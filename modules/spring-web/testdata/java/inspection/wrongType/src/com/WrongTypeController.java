package com.example.springcoredemo.web;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

@RestController
public class WrongTypeController {

    private WrongTypeService service;

    @PutMapping(value = {"/product"})
    @ResponseBody
    public Mono<String> product() {
        return service.simple();
    }

}
