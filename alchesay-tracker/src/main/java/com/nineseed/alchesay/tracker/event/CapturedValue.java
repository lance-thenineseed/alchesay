package com.nineseed.alchesay.tracker.event;

import lombok.NonNull;

/**
 * An immutable, named value captured from a JSON document.
 * <p>
 * Instances are normally produced by {@link ValueReference#capture}, which resolves a JSON Pointer
 * path and converts the result to a plain Java type (see {@link ValueReference} for the type mapping).
 * Equality is based on {@code name} and {@code value}.
 *
 * @param name  The name identifying this captured value.
 * @param value The captured value, converted to a Java type. Never {@code null}; a JSON {@code null} is
 *              represented by {@link #NULL_VALUE} and a missing path by {@link #MISSING_VALUE}.
 */
public record CapturedValue(String name, Object value) {
    /**
     * Sentinel value used when the path does not exist in the captured document.
     * Distinct from {@link #NULL_VALUE}, which indicates the path exists with a JSON {@code null} value.
     * Compare by identity or {@code equals}.
     */
    public static final Object MISSING_VALUE = new Object() {
        @Override
        public String toString() {
            return "MISSING_VALUE";
        }
    };

    /**
     * Sentinel value used when the path exists in the captured document but holds a JSON {@code null}.
     * Distinct from {@link #MISSING_VALUE}, which indicates the path does not exist.
     * Compare by identity or {@code equals}.
     */
    public static final Object NULL_VALUE = new Object() {
        @Override
        public String toString() {
            return "NULL_VALUE";
        }
    };

    /**
     * Creates a captured value.
     *
     * @param name  the name identifying this value
     * @param value the captured value; use {@link #NULL_VALUE} or {@link #MISSING_VALUE} rather than {@code null}
     * @throws NullPointerException if {@code name} or {@code value} is {@code null}
     */
    public CapturedValue(@NonNull String name, @NonNull Object value) {
        this.name = name;
        this.value = value;
    }
}
