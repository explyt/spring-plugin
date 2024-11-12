import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mail")
public class TestConfig {

    private String hostName;
    private int port;
    private String from;
    private long ert;

    private String newProperty;
    private ExternalSettings externalSettings;

    private NestedSettings nestedSettings = new NestedSettings();

    public String getNewProperty() {
        return newProperty;
    }

    public void setNewProperty(String newProperty) {
        this.newProperty = newProperty;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public ExternalSettings getExternalSettings() {
        return externalSettings;
    }

    public void setExternalSettings(ExternalSettings externalSettings) {
        this.externalSettings = externalSettings;
    }

    public void setFormAndPort(String formAndPort) {
        from = formAndPort;
        port = 8081;
    }

    public long getErt() {
        return ert;
    }

    public void setErt(long ert) {
        this.ert = ert;
    }

    public NestedSettings getNestedSettings() {
        return nestedSettings;
    }

    public void setNestedSettings(NestedSettings nestedSettings) {
        this.nestedSettings = nestedSettings;
    }

    private class NestedSettings {
        private String f1;
        private AnotherNestedSettings anotherNestedSettings = new AnotherNestedSettings("", "", "");

        public String getF1() {
            return f1;
        }

        public void setF1(String f1) {
            this.f1 = f1;
        }

        public AnotherNestedSettings getAnotherNestedSettings() {
            return anotherNestedSettings;
        }

        public void setAnotherNestedSettings(AnotherNestedSettings anotherNestedSettings) {
            this.anotherNestedSettings = anotherNestedSettings;
        }
    }

    public record AnotherNestedSettings(String camelCaseLongPropertyVeryLongProperty,
                                        String property2,
                                        String property3) {
    }

    //    public String getFormAndPort() {
//        return formAndPort;
//    }
}
