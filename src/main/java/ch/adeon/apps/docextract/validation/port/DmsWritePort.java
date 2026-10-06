package ch.adeon.apps.docextract.validation.port;

import java.util.Map;

public interface DmsWritePort {
    void writeAttributes(String documentId, Map<String, Object> attributes);
}
