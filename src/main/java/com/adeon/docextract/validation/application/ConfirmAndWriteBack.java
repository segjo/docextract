package com.adeon.docextract.validation.application;

import com.adeon.docextract.validation.domain.ConfirmationCommand;
import com.adeon.docextract.validation.domain.ConfirmationResult;

public interface ConfirmAndWriteBack {
    ConfirmationResult confirm(ConfirmationCommand command);
}
