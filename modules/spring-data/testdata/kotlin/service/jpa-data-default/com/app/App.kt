package com.app;

import data.jpa.TestRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;

@SpringBootApplication
class App() {
    @Autowired
    lateinit var testRepo: TestRepo
}


fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

