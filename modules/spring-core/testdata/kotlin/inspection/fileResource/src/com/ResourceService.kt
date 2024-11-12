package com

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileUrlResource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.net.MalformedURLException

@Service
class ResourceService {
    @Autowired
    var resourceLoader: ResourceLoader? = null

    @Autowired
    var applicationContext: ApplicationContext? = null

    @PostConstruct
    @Throws(MalformedURLException::class)
    fun init() {
        ClassPathResource("classpath:application1.properties")
        ResourceService::class.java.getResource("application1.properties")
        ResourceService::class.java.classLoader.getResource("/application1.properties")
        FileUrlResource("classpath:application1.properties")
        resourceLoader!!.getResource("classpath:application1.properties")
        applicationContext!!.getResource("classpath:application1.properties")
    }
}

@Service
class ResourceServiceVariables {
    @Autowired
    var resourceLoader: ResourceLoader? = null

    @Autowired
    var applicationContext: ApplicationContext? = null

    @PostConstruct
    @Throws(MalformedURLException::class)
    fun init() {
        val PATH = "application1.properties"
        val PATH1 = PATH
        val PATH2 = "/$PATH1"
        val PATH3 = "classpath:application1.properties"

        ClassPathResource(PATH3)
        ResourceServiceVariables::class.java.getResource(PATH1)
        ResourceServiceVariables::class.java.getClassLoader().getResource(PATH2)
        FileUrlResource(PATH3)
        resourceLoader!!.getResource(PATH3)
        applicationContext!!.getResource(PATH3)
    }
}

@Service
class ResourceServiceFields {
    val PATH: String = "application1.properties"
    val PATH1: String = PATH
    val PATH2: String = "/$PATH1"
    val PATH3: String = "classpath:application1.properties"

    @Autowired
    var resourceLoader: ResourceLoader? = null

    @Autowired
    var applicationContext: ApplicationContext? = null

    @PostConstruct
    @Throws(MalformedURLException::class)
    fun init() {
        ClassPathResource(PATH3)
        ResourceServiceFields::class.java.getResource(PATH1)
        ResourceServiceFields::class.java.getClassLoader().getResource(PATH2)
        FileUrlResource(PATH3)
        resourceLoader!!.getResource(PATH3)
        applicationContext!!.getResource(PATH3)
    }
}