package config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "main")
public class TestConfig {

    private Boolean enabled = false;
    private boolean enabledPrimitive = false;

    private Integer maxIntegerValue;

    private Double maxDoubleValue;

    private Number maxNumberValue;

    private Integer maxConnections;

    private Integer[] arrayInteger;

    private Map<Integer, Integer> contexts;

    private List<Integer> listNotInteger = new ArrayList<>();

    private List<Integer> addresses = new ArrayList<>();

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabledPrimitive() {
        return enabledPrimitive;
    }

    public void setEnabledPrimitive(boolean enabled) {
        this.enabledPrimitive = enabled;
    }

    public Integer getMaxIntegerValue() {
        return maxIntegerValue;
    }

    public void setMaxIntegerValue(Integer maxIntegerValue) {
        this.maxIntegerValue = maxIntegerValue;
    }

    public Double getMaxDoubleValue() {
        return maxDoubleValue;
    }

    public void setMaxDoubleValue(Double maxDoubleValue) {
        this.maxDoubleValue = maxDoubleValue;
    }

    public Number getMaxNumberValue() {
        return maxNumberValue;
    }

    public void setMaxNumberValue(Number maxNumberValue) {
        this.maxNumberValue = maxNumberValue;
    }

    @Deprecated
    @DeprecatedConfigurationProperty(reason = "Deprecated because that's all.", replacement = "not.main")
    public Integer getMaxConnections() {
        return maxConnections;
    }

    @Deprecated
    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer[] getArrayInteger() {
        return arrayInteger;
    }

    public void setArrayInteger(Integer[] arrayInteger) {
        this.arrayInteger = arrayInteger;
    }

    public Map<Integer, Integer> getContexts() {
        return contexts;
    }

    public void setContexts(java.util.Map<Integer, Integer> contexts) {
        this.contexts = contexts;
    }

    public List<Integer> getListNotInteger() {
        return listNotInteger;
    }

    public void setListNotInteger(java.util.List<Integer> listNotInteger) {
        this.listNotInteger = listNotInteger;
    }

    public List<Integer> getAddresses() {
        return addresses;
    }

    public void setAddresses(java.util.List<Integer> addresses) {
        this.addresses = addresses;
    }

}