package com.app;

import data.jpa.TestRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import java.util.*;

@SpringBootApplication
public class App {
    
    @Autowired
    TestRepo testRepo;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}

