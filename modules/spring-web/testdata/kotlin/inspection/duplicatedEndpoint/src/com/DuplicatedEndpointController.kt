package com.example.springcoredemo.web

import org.springframework.web.bind.annotation.*

@RestController
class DuplicatedEndpointController {

    @RequestMapping(path = "/same/uri", method = RequestMethod.GET)
    @ResponseBody
    fun conflictedRequestMapping(): String {
        return "conflictedRequestMapping"
    }

    @GetMapping("/same/uri")
    @ResponseBody
    fun conflictedGet(): String {
        return "conflictedGet"
    }

    @PostMapping("/same/uri")
    @ResponseBody
    fun postWithoutConflict(): String {
        return "postWithoutConflict"
    }

}