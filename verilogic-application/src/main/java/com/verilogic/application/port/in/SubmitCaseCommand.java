package com.verilogic.application.port.in;

import java.util.Objects;

/**
 * Inbound command object to submit an application for neurosymbolic verification.
 */
public record SubmitCaseCommand(
        String rawUnstructuredText,
        String requestedBy,
        boolean dryRun
) {
    public SubmitCaseCommand {
        Objects.requireNonNull(rawUnstructuredText, "rawUnstructuredText cannot be null");
        Objects.requireNonNull(requestedBy, "requestedBy cannot be null");
        if (rawUnstructuredText.isBlank()) {
            throw new IllegalArgumentException("rawUnstructuredText cannot be blank");
        }
    }
}
