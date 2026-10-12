package dev.shristi.rag.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Masks common identifier patterns before text is indexed or sent to an
 * external LLM. This is a defense-in-depth example, not a certified HIPAA
 * de-identification method (Safe Harbor covers 18 identifier types).
 */
public class PhiRedactor {

    private final Map<String, Pattern> rules = new LinkedHashMap<>();

    public PhiRedactor() {
        rules.put("[SSN]", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        rules.put("[EMAIL]", Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.]+\\b"));
        rules.put("[PHONE]", Pattern.compile("\\(?\\b\\d{3}\\)?[-. ]?\\d{3}[-. ]\\d{4}\\b"));
        rules.put("[MRN]", Pattern.compile("\\bMRN[:#]?\\s*\\d{6,10}\\b", Pattern.CASE_INSENSITIVE));
        rules.put("[DATE]", Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b"));
    }

    public String redact(String text) {
        String result = text;
        for (Map.Entry<String, Pattern> rule : rules.entrySet()) {
            result = rule.getValue().matcher(result).replaceAll(rule.getKey());
        }
        return result;
    }
}
