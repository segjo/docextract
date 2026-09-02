package com.adeon.docextract.validation.adapter.in.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adeon.docextract.validation.application.ConfirmAndWriteBack;
import com.adeon.docextract.validation.domain.ConfirmationCommand;
import com.adeon.docextract.validation.domain.ConfirmationResult;

@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    private final ConfirmAndWriteBack confirmAndWriteBack;

    public ValidationController(ConfirmAndWriteBack confirmAndWriteBack) {
        this.confirmAndWriteBack = confirmAndWriteBack;
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmationResult> confirm(@RequestBody ConfirmRequest request) {
        ConfirmationResult result = confirmAndWriteBack.confirm(
                new ConfirmationCommand(request.documentId(), request.consentGiven(), request.attributes()));
        return ResponseEntity.ok(result);
    }

    public record ConfirmRequest(String documentId, boolean consentGiven, Map<String, Object> attributes) {
    }
}
