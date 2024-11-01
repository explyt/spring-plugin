package com.example.springcoredemo.web;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class DuplicatedEndpointController {
    
    @RequestMapping(path = "/same/uri", method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    public String conflictedTwice() {
        return "conflictedTwice";
    }
    
    @GetMapping("/same/uri")
    @ResponseBody
    public String conflictedGet() {
        return "conflictedGet";
    }
    
    @PutMapping("/same/uri")
    @ResponseBody
    public String conflictedPut() {
        return "conflictedPut";
    }
    
    @PostMapping("/same/uri")
    @ResponseBody
    public String postWithoutConflict() {
        return "postWithoutConflict";
    }
    
}
