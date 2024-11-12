package com.app;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;


@SpringBootApplication
@Import(com.outer.OuterImport::class)
class Application
