package com.app;

import data.jpa.TestRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import java.util.*;

@SpringBootApplication
@EnableJpaRepositories("data.jpa")
@EntityScan("data.jpa.*")
class App() {
    @Autowired
    lateinit var testRepo: TestRepo
}


fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

