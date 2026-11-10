package ch.adeon.apps.docextract.security.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Test vector of the d.velop documentation (x-dv-sig-1, doc/d-velop-developer.md). */
class TenantSignatureTest {

  static final byte[] SECRET =
      Base64.getDecoder().decode("ptuQ0b0BskmLLxXsjjhH9Su8ozTvZl6Z/5/HlaORoRg=");
  static final String TENANT_ID = "a12be5";
  static final String BASE_URI = "https://header.example.com";
  static final String SIGNATURE = "Zjcf28p5aQ6amtbs6s9b9cPyBPdziwUslR2DZqaGUTQ=";

  @Test
  void matches_the_documented_test_vector() {
    assertThat(TenantSignature.matches(SECRET, BASE_URI, TENANT_ID, SIGNATURE)).isTrue();
  }

  @Test
  void differs_for_another_tenant_or_base_uri() {
    assertThat(TenantSignature.matches(SECRET, BASE_URI, "other", SIGNATURE)).isFalse();
    assertThat(TenantSignature.matches(SECRET, "https://other.example.com", TENANT_ID, SIGNATURE))
        .isFalse();
  }

  @Test
  void differs_for_another_secret() {
    assertThat(TenantSignature.matches(new byte[] {1, 2, 3}, BASE_URI, TENANT_ID, SIGNATURE))
        .isFalse();
  }

  @Test
  void malformed_or_empty_signature_never_matches() {
    assertThat(TenantSignature.matches(SECRET, BASE_URI, TENANT_ID, "###")).isFalse();
    assertThat(TenantSignature.matches(SECRET, BASE_URI, TENANT_ID, "")).isFalse();
  }
}
