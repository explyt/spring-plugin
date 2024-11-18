package com.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "main")
@Configuration
public class MainFooProperties {
    /**
     * max Sessions Per Connection
     * */
    private Integer maxSessionsPerConnection = 765;

    /**
     * Event listener
     */
    private String eventListener;

    public Integer getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    public void setMaxSessionsPerConnection(Integer maxSessionsPerConnection) {
        this.maxSessionsPerConnection = maxSessionsPerConnection;
    }

    public String getEventListener() {
        return eventListener;
    }

    public void setEventListener(String eventListener) {
        this.eventListener = eventListener;
    }

}