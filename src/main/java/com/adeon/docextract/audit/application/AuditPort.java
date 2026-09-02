package com.adeon.docextract.audit.application;

import com.adeon.docextract.audit.domain.AuditEvent;

public interface AuditPort {
    void append(AuditEvent event);
}
