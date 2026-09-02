package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;

public interface ConfirmAndWriteBack {
    ConfirmationResult confirm(ConfirmationCommand command);
}
