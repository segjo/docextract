package ch.adeon.apps.docextract.shared.http;

import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The JDK {@link HttpClient} defaults to offering an {@code Upgrade: h2c} cleartext HTTP/2 upgrade
 * alongside every request. Some plain HTTP/1.1 servers (e.g. docling-serve's uvicorn/Starlette)
 * mishandle that upgrade offer and fail to read the request body at all — observed as a multipart
 * file "field required" error even though the bytes on the wire are well-formed. Pin outbound
 * local-service clients to HTTP/1.1 to avoid this.
 */
public final class Http1RestClientFactory {

  private Http1RestClientFactory() {}

  public static RestClient create() {
    HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient)).build();
  }
}
