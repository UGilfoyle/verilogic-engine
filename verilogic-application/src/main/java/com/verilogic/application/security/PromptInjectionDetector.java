package com.verilogic.application.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security filter detecting and neutralizing adversarial prompt injections and evasion attacks.
 * Prevents adversarial jailbreaks from attempting to manipulate LLM extraction logic.
 */
public class PromptInjectionDetector {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|prompts|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|prior|above)\\s+guidelines", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(in\\s+)?(developer\\s+mode|dan\\s+mode|unrestricted)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)bypass\\s+(all\\s+)?(constraints|rules|policies|solvers|verification)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)override\\s+(dti|credit\\s*score|invariants|solvency)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)always\\s+(approve|certify|pass)\\s+this\\s+case", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)---BEGIN\\s+(SYSTEM|ADMIN)\\s+OVERRIDE---", Pattern.CASE_INSENSITIVE)
    );

    // Matches hidden Unicode zero-width spaces, joiners, and bidirectional overrides
    private static final Pattern HIDDEN_UNICODE_PATTERN = Pattern.compile("[\\u200B-\\u200D\\uFEFF\\u202A-\\u202E]");

    public record SecurityScanResult(
            boolean threatDetected,
            List<String> detectedThreats,
            String sanitizedText
    ) {}

    /**
     * Scans and sanitizes raw input text against adversarial prompt injections.
     */
    public static SecurityScanResult scan(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return new SecurityScanResult(false, List.of(), "");
        }

        // 1. Strip invisible zero-width unicode characters used for token obfuscation
        String sanitized = HIDDEN_UNICODE_PATTERN.matcher(rawInput).replaceAll("");

        // 2. Scan for adversarial injection patterns
        List<String> detectedThreats = new ArrayList<>();
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                detectedThreats.add("Detected Injection Pattern: [" + pattern.pattern() + "]");
            }
        }

        boolean hasThreat = !detectedThreats.isEmpty();
        return new SecurityScanResult(hasThreat, detectedThreats, sanitized);
    }
}
