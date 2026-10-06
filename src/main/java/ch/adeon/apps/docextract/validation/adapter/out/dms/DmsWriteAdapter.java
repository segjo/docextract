package ch.adeon.apps.docextract.validation.adapter.out.dms;

import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DmsWriteAdapter implements DmsWritePort {

  @Override
  public void writeAttributes(String documentId, Map<String, Object> attributes) {
    // outbound adapter implementation placeholder
  }
}
