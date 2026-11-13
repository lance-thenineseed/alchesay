package com.nineseed.alchesay.tracker.rules;

import com.nineseed.alchesay.tracker.event.CapturedValue;
import com.nineseed.alchesay.tracker.event.ValueComparison;
import com.nineseed.alchesay.tracker.event.ValueReference;
import com.nineseed.alchesay.tracker.workflow.Context;
import lombok.NonNull;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A rule that captures a value from a JSON document and compares it against a value to match.
 * <p>
 * A string value to match may contain {@code ${name}} placeholders, which are resolved from the {@link Context}
 * each time the rule is tested:
 * <ul>
 *     <li>When the placeholder is the whole string, as in {@code "${orderId}"}, the context value is used
 *     as is, so a number stays a number.</li>
 *     <li>Otherwise each placeholder is replaced by the context value's text, as in {@code "ORD-${orderId}"}.
 *     Only string, number and boolean context values can be embedded this way.</li>
 * </ul>
 * A backslash makes the next character literal, so {@code "\\${count\\}"} matches the text {@code "${count}"}
 * and {@code "\\\\"} matches a single backslash. A backslash at the end of the string is kept as is.
 */
public class MatchingRule implements Rule {
    /** Marks a value to match that could not be resolved from the context. */
    private static final Object UNRESOLVED = new Object();

    private final ValueReference valueReference;
    private final ValueComparison comparison;
    private final Object valueToMatch;
    /** Text between placeholders; always one more entry than {@link #placeholderNames}. */
    private final List<String> literals = new ArrayList<>();
    private final List<String> placeholderNames = new ArrayList<>();

    /**
     * Creates a matching rule.
     *
     * @param valueReference the value to capture from the document
     * @param comparison     the comparison to apply, with the captured value on the left-hand side
     * @param valueToMatch   the value to compare against, optionally containing {@code ${name}} placeholders
     *                       and backslash escapes
     */
    public MatchingRule(@NonNull ValueReference valueReference, @NonNull ValueComparison comparison,
                        Object valueToMatch) {
        this.valueReference = valueReference;
        this.comparison = comparison;
        this.valueToMatch = valueToMatch;
        if (valueToMatch instanceof String template) {
            parse(template);
        }
    }

    /**
     * Splits {@code template} into {@link #literals} and {@link #placeholderNames}, removing escapes. A
     * {@code ${} without a closing brace, or with an empty name, is kept as literal text.
     */
    private void parse(String template) {
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length()) {
                literal.append(template.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '$' && template.startsWith("{", i + 1)) {
                int close = template.indexOf('}', i + 2);
                if (close > i + 2) {
                    literals.add(literal.toString());
                    literal.setLength(0);
                    placeholderNames.add(template.substring(i + 2, close));
                    i = close + 1;
                    continue;
                }
            }
            literal.append(c);
            i++;
        }
        literals.add(literal.toString());
    }

    /**
     * Captures the referenced value from {@code rootNode} and compares it against the value to match.
     * A placeholder whose name is not in {@code context}, or whose value cannot be embedded, never matches.
     *
     * @return the result of {@link CapturedValue#compare}, or {@code false} for an unresolved placeholder
     */
    @Override
    public boolean test(@NonNull JsonNode rootNode, @NonNull Context context) {
        Object expected = resolve(context);
        return expected != UNRESOLVED && valueReference.capture(rootNode).compare(comparison, expected);
    }

    private Object resolve(Context context) {
        if (placeholderNames.isEmpty()) {
            return valueToMatch instanceof String ? literals.getFirst() : valueToMatch;
        }
        if (placeholderNames.size() == 1 && literals.getFirst().isEmpty() && literals.getLast().isEmpty()) {
            Object value = context.find(placeholderNames.getFirst()).value();
            return value == CapturedValue.MISSING_VALUE ? UNRESOLVED : value;
        }
        StringBuilder resolved = new StringBuilder(literals.getFirst());
        for (int i = 0; i < placeholderNames.size(); i++) {
            Object value = context.find(placeholderNames.get(i)).value();
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                return UNRESOLVED;
            }
            resolved.append(value).append(literals.get(i + 1));
        }
        return resolved.toString();
    }
}
