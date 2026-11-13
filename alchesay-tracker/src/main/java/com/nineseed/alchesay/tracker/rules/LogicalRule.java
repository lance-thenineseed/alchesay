package com.nineseed.alchesay.tracker.rules;

import com.nineseed.alchesay.tracker.workflow.Context;
import lombok.NonNull;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * A rule that combines other rules with a {@link LogicalOperator}. Rules are evaluated in order and
 * evaluation stops as soon as the result is known.
 */
public class LogicalRule implements Rule {
    private final List<Rule> rules;
    private final LogicalOperator operator;

    /**
     * Creates a logical rule.
     *
     * @param rules    the rules to combine, in evaluation order
     * @param operator how to combine the rules' results
     * @throws NullPointerException     if {@code rules}, any rule, or {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code rules} is empty
     */
    public LogicalRule(@NonNull List<Rule> rules, @NonNull LogicalOperator operator) {
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("A logical rule needs at least one rule to combine");
        }
        this.rules = List.copyOf(rules);
        this.operator = operator;
    }

    /**
     * @return for {@link LogicalOperator#AND}, whether every rule matches; for {@link LogicalOperator#OR},
     *         whether any rule matches
     */
    @Override
    public boolean test(@NonNull JsonNode rootNode, @NonNull Context context) {
        return switch (operator) {
            case AND -> rules.stream().allMatch(rule -> rule.test(rootNode, context));
            case OR -> rules.stream().anyMatch(rule -> rule.test(rootNode, context));
        };
    }
}
