package com.example.springcoredemo.web;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@RestController
public class InitBinderController {
    
    @InitBinder("valid")
    public void validBinderMethod(WebDataBinder dataBinder) {}
    
    @InitBinder("invalid")
    public String invalidBinderMethod(WebDataBinder dataBinder) {
        return "";
    }
    
}
