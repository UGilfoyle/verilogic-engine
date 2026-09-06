package com.verilogic.ui.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.verilogic.application.port.in.SubmitCaseCommand;

/**
 * REST request body for raw-text underwriting. Accepts both {@code requestedBy}
 * and the documented alias {@code submitterId}.
 */
public record EvaluateCaseRequest(
        String rawUnstructuredText,
        @JsonAlias("submitterId") String requestedBy,
        boolean dryRun
) {
    public SubmitCaseCommand toCommand() {
        String actor = (requestedBy == null || requestedBy.isBlank()) ? "anonymous" : requestedBy;
        return new SubmitCaseCommand(rawUnstructuredText, actor, dryRun);
    }
}
