package com.adeon.docextract.audit.application;

import org.springframework.stereotype.Component;

import com.adeon.docextract.audit.domain.AuditEvent;

@Component
public class NoopAuditAdapter implements AuditPort {

    @Override
    public void append(AuditEvent event) {
        // append-only audit adapter placeholder
    }
}
