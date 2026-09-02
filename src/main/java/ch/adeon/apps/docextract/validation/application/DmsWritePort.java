package ch.adeon.apps.docextract.validation.application;

import java.util.Map;

public interface DmsWritePort {
    void writeAttributes(String documentId, Map<String, Object> attributes);
}
