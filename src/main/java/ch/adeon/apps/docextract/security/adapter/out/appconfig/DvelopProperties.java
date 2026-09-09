package ch.adeon.apps.docextract.security.adapter.out.appconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configures the jstore lookup and the properties-based fallback for the
 * d.velop system base URI.
 */
@Component
@ConfigurationProperties(prefix = "docextract.dvelop")
public class DvelopProperties {

    private String jstoreUri = "http://localhost:6380/store/httpgateway/appconfig";
    private String systemBaseUri;

    public String getJstoreUri() {
        return jstoreUri;
    }

    public void setJstoreUri(String jstoreUri) {
        this.jstoreUri = jstoreUri;
    }

    public String getSystemBaseUri() {
        return systemBaseUri;
    }

    public void setSystemBaseUri(String systemBaseUri) {
        this.systemBaseUri = systemBaseUri;
    }
}
