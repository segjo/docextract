package ch.adeon.apps.docextract.shared.hash;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Keyed HMAC-SHA256 hex digest, for pseudonyms that must not be recomputable without the key. */
public final class Hmac {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private Hmac() {}

  public static String sha256Hex(byte[] key, String data) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(key, HMAC_SHA256));
      return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HMAC-SHA256 is not available", ex);
    }
  }
}
