package ch.adeon.apps.docextract.shared.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Shared SHA-256 hex digest helper (used e.g. for document-duplicate detection, C-4 audit hashes).
 */
public final class Sha256 {

  private Sha256() {}

  public static String hex(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
