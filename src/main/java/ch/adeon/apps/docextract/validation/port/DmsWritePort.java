package ch.adeon.apps.docextract.validation.port;

import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.Map;

public interface DmsWritePort {
  String writeAttributes(
      String repositoryId,
      String contentLocationUri,
      Map<String, Object> attributes,
      String filename,
      String sourceCategory,
      DvelopCredential credential);
}
