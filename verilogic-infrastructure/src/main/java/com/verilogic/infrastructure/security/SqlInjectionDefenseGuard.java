package com.verilogic.infrastructure.security;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enterprise SQL Injection (SQLi) Defense Guard.
 * Inspects all parameters and JSON payloads for SQL injection markers,
 * union-based extraction, sleep/benchmark blind injections, and comment delimiters.
 */
public class SqlInjectionDefenseGuard {

    private static final List<Pattern> SQLI_PATTERNS = List.of(
            Pattern.compile("(?i)\\bUNION\\s+(ALL\\s+)?SELECT\\b"),
            Pattern.compile("(?i)['\"]?\\s*\\b(OR|AND)\\b\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+['\"]?"),
            Pattern.compile("(?i)(;?\\s*\\b(DROP|ALTER|TRUNCATE|DELETE)\\s+TABLE\\b)"),
            Pattern.compile("(?i)(--|/\\*|\\*/|;--)"),
            Pattern.compile("(?i)\\b(WAITFOR\\s+DELAY|SLEEP\\s*\\(\\d+\\)|BENCHMARK\\s*\\()"),
            Pattern.compile("(?i)\\bEXEC(UTE)?\\s*\\("),
            Pattern.compile("(?i)\\bSELECT\\s+.+\\s+FROM\\b")
    );

    public record SqlScanResult(
            boolean injectionDetected,
            String detectedVector,
            String cleanedInput
    ) {}

    public static SqlScanResult scan(String input) {
        if (input == null || input.isBlank()) {
            return new SqlScanResult(false, null, "");
        }

        for (Pattern pattern : SQLI_PATTERNS) {
            var matcher = pattern.matcher(input);
            if (matcher.find()) {
                return new SqlScanResult(true, "SQLi Pattern Matched: " + pattern.pattern(), input);
            }
        }

        return new SqlScanResult(false, null, input);
    }
}
