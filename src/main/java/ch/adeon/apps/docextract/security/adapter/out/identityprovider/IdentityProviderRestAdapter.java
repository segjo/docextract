package ch.adeon.apps.docextract.security.adapter.out.identityprovider;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.DvelopUser;

/**
 * Calls {@code GET /identityprovider/validate} to authenticate an incoming
 * AuthSessionId/Bearer token.
 */
@Component
public class IdentityProviderRestAdapter implements IdentityProviderPort {

    private final RestClient restClient;
    private final AppConfigPort appConfigPort;

    public IdentityProviderRestAdapter(AppConfigPort appConfigPort) {
        this.restClient = RestClient.create();
        this.appConfigPort = appConfigPort;
    }

    @Override
    public DvelopUser validateBearerToken(String bearerToken) {
        return validate(request -> request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken));
    }

    @Override
    public DvelopUser validateSessionCookie(String authSessionId) {
        return validate(request -> request.header(HttpHeaders.COOKIE, "AuthSessionId=" + authSessionId));
    }

    private DvelopUser validate(UnaryOperator<RestClient.RequestHeadersSpec<?>> credential) {
        try {
            UserDto response = credential.apply(restClient.get()
                    .uri(appConfigPort.systemBaseUri() + "/identityprovider/validate"))
                    .retrieve()
                    .body(UserDto.class);
            if (response == null) {
                throw new BadCredentialsException("Identityprovider returned no user for the given session");
            }
            return response.toDomain();
        } catch (HttpClientErrorException ex) {
            throw new BadCredentialsException("Session validation failed", ex);
        } catch (RestClientException ex) {
            throw new AuthenticationServiceException("Identityprovider is not reachable", ex);
        }
    }

    private record UserDto(String id, String userName, String displayName, List<ValueDto> groups) {

        DvelopUser toDomain() {
            List<String> groupIds = groups == null
                    ? List.of()
                    : groups.stream().map(ValueDto::value).filter(Objects::nonNull).toList();
            return new DvelopUser(id, userName, displayName, groupIds);
        }
    }

    private record ValueDto(String value, String display, String type) {
    }
}
