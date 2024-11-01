package com;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

@Service
public class ResourceService {
    @Autowired
    ResourceLoader resourceLoader;
    @Autowired
    ApplicationContext applicationContext;
    @PostConstruct
    public void init() throws MalformedURLException {
        ClassPathResource path = new ClassPathResource("classpath:application1.properties");
        ResourceService.class.getResource("application1.properties");
        ResourceService.class.getClassLoader().getResource("/application1.properties");
        AbstractFileResolvingResource pathFile = new FileUrlResource("classpath:application1.properties");
        Resource res = resourceLoader.getResource("classpath:application1.properties");
        applicationContext.getResource("classpath:application1.properties");
    }
}

@Service
public class ResourceServiceVariables {
    @Autowired
    ResourceLoader resourceLoader;
    @Autowired
    ApplicationContext applicationContext;
    @PostConstruct
    public void init() throws MalformedURLException {
        final String PATH = "application1.properties";
        final String PATH1 = PATH;
        final String PATH2 = "/" + PATH1;
        final String PATH3 = "classpath:application1.properties";

        ClassPathResource path = new ClassPathResource(PATH3);
        ResourceService.class.getResource(PATH1);
        ResourceService.class.getClassLoader().getResource(PATH2);
        AbstractFileResolvingResource pathFile = new FileUrlResource(PATH3);
        Resource res = resourceLoader.getResource(PATH3);
        applicationContext.getResource(PATH3);
    }
}

@Service
public class ResourceServiceFields {
    final String PATH = "application1.properties";
    final String PATH1 = PATH;
    final String PATH2 = "/" + PATH1;
    final String PATH3 = "classpath:application1.properties";

    @Autowired
    ResourceLoader resourceLoader;
    @Autowired
    ApplicationContext applicationContext;

    @PostConstruct
    public void init() throws MalformedURLException {
        ClassPathResource path = new ClassPathResource(PATH3);
        ResourceService.class.getResource(PATH1);
        ResourceService.class.getClassLoader().getResource(PATH2);
        AbstractFileResolvingResource pathFile = new FileUrlResource(PATH3);
        Resource res = resourceLoader.getResource(PATH3);
        applicationContext.getResource(PATH3);
    }
}