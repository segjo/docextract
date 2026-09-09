package ch.adeon.apps.docextract.security.adapter.out.appconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ch.adeon.apps.docextract.security.application.AppConfigPort;

/**
 * Resolves the d.velop {@code systembaseuri} from jstore
 * ({@code GET /store/httpgateway/appconfig}), falling back to
 * {@code docextract.dvelop.system-base-uri} when jstore is unreachable.
 * The successfully resolved value is cached for the lifetime of the instance.
 */
@Component
public class JstoreAppConfigAdapter implements AppConfigPort {

    private final RestClient restClient;
    private final DvelopProperties properties;
    private volatile String cachedSystemBaseUri;

    public JstoreAppConfigAdapter(DvelopProperties properties) {
        this.restClient = RestClient.create();
        this.properties = properties;
    }

    @Override
    public String systemBaseUri() {
        String cached = cachedSystemBaseUri;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedSystemBaseUri == null) {
                cachedSystemBaseUri = resolveSystemBaseUri();
            }
            return cachedSystemBaseUri;
        }
    }

    private String resolveSystemBaseUri() {
        String fromJstore = fetchFromJstore();
        if (StringUtils.hasText(fromJstore)) {
            return fromJstore;
        }
        if (StringUtils.hasText(properties.getSystemBaseUri())) {
            return properties.getSystemBaseUri();
        }
        throw new IllegalStateException(
                "systemBaseUri could not be resolved from jstore (%s) nor from docextract.dvelop.system-base-uri"
                        .formatted(properties.getJstoreUri()));
    }

    private String fetchFromJstore() {
        try {
            JstoreAppConfigResponse response = restClient.get()
                    .uri(properties.getJstoreUri())
                    .retrieve()
                    .body(JstoreAppConfigResponse.class);
            return response == null || response.appConfiguration() == null
                    ? null
                    : response.appConfiguration().systembaseuri();
        } catch (RestClientException ex) {
            return null;
        }
    }

    private record JstoreAppConfigResponse(@JsonProperty("AppConfiguration") AppConfiguration appConfiguration) {

        private record AppConfiguration(@JsonProperty("systembaseuri") String systembaseuri) {
        }
    }
}
