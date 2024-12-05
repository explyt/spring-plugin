package src;

import src.WeekEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeType;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "main.local")
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

    /**
     * A mime type
     */
    private MimeType codeMimeType;

    /**
     * A enum type
     */
    private WeekEnum enumValue;

    private Locale codeLocale;

    private Charset codeCharset;

    private Resource codeResource;

    private Map<String, Integer> contexts;

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

    public MimeType getCodeMimeType() {
        return codeMimeType;
    }

    public void setCodeMimeType(MimeType codeMimeType) {
        this.codeMimeType = codeMimeType;
    }

    public WeekEnum getEnumValue() {
        return enumValue;
    }

    public void setEnumValue(WeekEnum enumValue) {
        this.enumValue = enumValue;
    }

    public Locale getCodeLocale() {
        return codeLocale;
    }

    public void setCodeLocale(Locale codeLocale) {
        this.codeLocale = codeLocale;
    }

    public Charset getCodeCharset() {
        return codeCharset;
    }

    public void setCodeCharset(Charset codeCharset) {
        this.codeCharset = codeCharset;
    }

    public Resource getCodeResource() {
        return codeResource;
    }

    public void setCodeResource(Resource codeResource) {
        this.codeResource = codeResource;
    }

    public Map<String, Integer> getContexts() {
        return contexts;
    }

    public void setContexts(Map<String, Integer> contexts) {
        this.contexts = contexts;
    }

}