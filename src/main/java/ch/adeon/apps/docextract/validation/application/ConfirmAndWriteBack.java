package ch.adeon.apps.docextract.validation.application;


public interface ConfirmAndWriteBack {
    ConfirmationResult confirm(ConfirmationCommand command);
}
