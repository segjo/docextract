package ch.adeon.apps.docextract.security.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signature of the d.velop tenant headers ({@code x-dv-sig-1}): HMAC-SHA256 with the app secret.
 */
final class TenantSignature {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private TenantSignature() {}

  /** HMAC-SHA256 over {@code baseUri + tenantId}, keyed with the base64-decoded app secret. */
  static byte[] sign(byte[] secret, String baseUri, String tenantId) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret, HMAC_SHA256));
      return mac.doFinal((baseUri + tenantId).getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HMAC-SHA256 is not available", ex);
    }
  }

  /** Constant-time comparison with the base64 encoded header value; malformed base64 is false. */
  static boolean matches(byte[] secret, String baseUri, String tenantId, String signature) {
    byte[] provided;
    try {
      provided = Base64.getDecoder().decode(signature);
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return MessageDigest.isEqual(sign(secret, baseUri, tenantId), provided);
  }
}
