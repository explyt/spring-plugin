package src;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeType;

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

}