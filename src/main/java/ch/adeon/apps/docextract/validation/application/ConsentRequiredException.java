package ch.adeon.apps.docextract.validation.application;

public class ConsentRequiredException extends RuntimeException {

    public ConsentRequiredException() {
        super("Consent is required before write-back");
    }
}
