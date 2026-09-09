package ch.adeon.apps.docextract.validation.adapter.out.dms;

import java.util.Map;

import org.springframework.stereotype.Component;

import ch.adeon.apps.docextract.validation.application.DmsWritePort;

@Component
public class DmsWriteAdapter implements DmsWritePort {

    @Override
    public void writeAttributes(String documentId, Map<String, Object> attributes) {
        // outbound adapter implementation placeholder
    }
}
