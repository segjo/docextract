package com.adeon.docextract.validation.domain;

import java.util.Map;

public record ConfirmationCommand(String documentId, boolean consentGiven, Map<String, Object> attributes) {
}
