package config

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;

@Configuration
@ConfigurationProperties(prefix = "main")
public class TestConfig {

    private Boolean enabled = false;
    private boolean enabledPrimitive = false;

    private Integer maxIntegerValue;

    private Double maxDoubleValue;

    private Number maxNumberValue;

    private Integer maxConnections;

    public Boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(java.lang.Boolean enabled) {
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

    public void setMaxIntegerValue(java.lang.Integer maxIntegerValue) {
        this.maxIntegerValue = maxIntegerValue;
    }

    public Double getMaxDoubleValue() {
        return maxDoubleValue;
    }

    public void setMaxDoubleValue(java.lang.Double maxDoubleValue) {
        this.maxDoubleValue = maxDoubleValue;
    }

    public Number getMaxDoubleValue() {
        return maxNumberValue;
    }

    public void setMaxNumberValue(java.lang.Number maxNumberValue) {
        this.maxNumberValue = maxNumberValue;
    }

    @java.lang.Deprecated
    @DeprecatedConfigurationProperty(reason = "Deprecated because that's all.", replacement = "not.main")
    public Integer getMaxConnections() {
        return maxConnections;
    }

    @java.lang.Deprecated
    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

}