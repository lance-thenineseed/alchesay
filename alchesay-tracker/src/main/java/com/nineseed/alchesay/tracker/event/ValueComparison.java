package com.nineseed.alchesay.tracker.event;

/**
 * The comparisons supported by {@link CapturedValue#compare(ValueComparison, Object)}, with the captured value on the
 * left-hand side. See {@link CapturedValue#compare(ValueComparison, Object)} for how each applies to numbers, strings
 * and collections.
 */
public enum ValueComparison {
    /**
     * Captured value is greater than the given value, numerically or case-insensitively for strings.
     */
    GREATER_THAN,
    /**
     * Captured value is greater than or equal to the given value, numerically or case-insensitively for strings.
     */
    GREATER_THAN_OR_EQUAL,
    /**
     * Captured value is less than the given value, numerically or case-insensitively for strings.
     */
    LESS_THAN,
    /**
     * Captured value is less than or equal to the given value, numerically or case-insensitively for strings.
     */
    LESS_THAN_OR_EQUAL,
    /**
     * Captured value equals the given value; numbers are compared by value regardless of type.
     */
    EQUALS,
    /**
     * Like {@link #EQUALS}, but strings are compared ignoring case.
     */
    EQUALS_IGNORE_CASE,
    /**
     * Negation of {@link #EQUALS}.
     */
    NOT_EQUALS,
    /**
     * Negation of {@link #EQUALS_IGNORE_CASE}.
     */
    NOT_EQUALS_IGNORE_CASE,
    /**
     * Captured string contains the given string, or captured collection contains an element {@link #EQUALS} to it.
     */
    CONTAINS,
    /**
     * Like {@link #CONTAINS}, but strings and collection elements are compared ignoring case.
     */
    CONTAINS_IGNORE_CASE,
    /**
     * Captured string starts with the given string.
     */
    STARTS_WITH,
    /**
     * Like {@link #STARTS_WITH}, but ignoring case.
     */
    STARTS_WITH_IGNORE_CASE,
    /**
     * Captured string ends with the given string.
     */
    ENDS_WITH,
    /**
     * Like {@link #ENDS_WITH}, but ignoring case.
     */
    ENDS_WITH_IGNORE_CASE,
}
