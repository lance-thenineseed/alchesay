package com.nineseed.alchesay.tracker.event;

import lombok.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;

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
     * The comparisons supported by {@link #compare(Comparison, Object)}, with the captured value on the
     * left-hand side. See {@link #compare(Comparison, Object)} for how each applies to numbers, strings
     * and collections.
     */
    public enum Comparison {
        /** Captured value is greater than the given value, numerically or case-insensitively for strings. */
        GREATER_THAN,
        /** Captured value is greater than or equal to the given value, numerically or case-insensitively for strings. */
        GREATER_THAN_OR_EQUAL,
        /** Captured value is less than the given value, numerically or case-insensitively for strings. */
        LESS_THAN,
        /** Captured value is less than or equal to the given value, numerically or case-insensitively for strings. */
        LESS_THAN_OR_EQUAL,
        /** Captured value equals the given value; numbers are compared by value regardless of type. */
        EQUALS,
        /** Like {@link #EQUALS}, but strings are compared ignoring case. */
        EQUALS_IGNORE_CASE,
        /** Negation of {@link #EQUALS}. */
        NOT_EQUALS,
        /** Negation of {@link #EQUALS_IGNORE_CASE}. */
        NOT_EQUALS_IGNORE_CASE,
        /** Captured string contains the given string, or captured collection contains an element {@link #EQUALS} to it. */
        CONTAINS,
        /** Like {@link #CONTAINS}, but strings and collection elements are compared ignoring case. */
        CONTAINS_IGNORE_CASE,
        /** Captured string starts with the given string. */
        STARTS_WITH,
        /** Like {@link #STARTS_WITH}, but ignoring case. */
        STARTS_WITH_IGNORE_CASE,
        /** Captured string ends with the given string. */
        ENDS_WITH,
        /** Like {@link #ENDS_WITH}, but ignoring case. */
        ENDS_WITH_IGNORE_CASE,
    }

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

    /**
     * Compares this captured value against another value.
     * <p>
     * When either side is a {@link Number}, both sides are compared by numeric value regardless of type,
     * and a numeric {@link String} on the other side is parsed as a number. For example, {@code 42} equals
     * {@code 42L}, {@code 42.0}, {@code "42"} and {@code "42.0"}. Otherwise values are compared with
     * {@link Object#equals}. {@link Comparison#EQUALS_IGNORE_CASE} ignores case when both sides are strings
     * and otherwise behaves like {@link Comparison#EQUALS}.
     * <p>
     * {@code STARTS_WITH}, {@code ENDS_WITH} and {@code CONTAINS} (and their ignore-case variants) apply only
     * when both sides are strings; for any other types they return {@code false}. As with {@link String#contains},
     * every string starts with, ends with and contains the empty string.
     * <p>
     * When this captured value is a {@link Collection}, {@code CONTAINS} is true if any element is equal to
     * {@code value} under {@code EQUALS} rules ({@code EQUALS_IGNORE_CASE} rules for {@code CONTAINS_IGNORE_CASE}),
     * so a list holding {@code 42} contains {@code 42L}, {@code 42.0} and {@code "42"}. A captured {@link Map}
     * (a JSON object) never contains anything; reference its keys directly with a JSON Pointer instead.
     * <p>
     * {@code GREATER_THAN}, {@code GREATER_THAN_OR_EQUAL}, {@code LESS_THAN} and {@code LESS_THAN_OR_EQUAL}
     * compare by numeric value under the same rules as {@code EQUALS}, so {@code 42.1} is greater than
     * {@code 40} and {@code "40"}. When both sides are strings they are ordered lexicographically, ignoring
     * case, so {@code "limestone"} is greater than {@code "Aardvark"}. Strings that differ only in case are
     * therefore equal for ordering purposes: {@code "Limestone"} is both {@code GREATER_THAN_OR_EQUAL} and
     * {@code LESS_THAN_OR_EQUAL} to {@code "limestone"}, even though they are not {@code EQUALS}. String ordering
     * compares character codes (see {@link String#compareToIgnoreCase}), not locale-sensitive collation, so for
     * example {@code "é"} sorts after {@code "z"}. Otherwise the ordering comparisons return {@code false}.
     *
     * @param comparison the comparison to apply, with this captured value on the left-hand side
     * @param value      the value to compare against
     * @return the result of the comparison
     * @throws IllegalArgumentException for {@code CONTAINS} or {@code CONTAINS_IGNORE_CASE} when
     *                                  {@code value} is a {@link Map}
     */
    public boolean compare(@NonNull Comparison comparison, Object value) {
        return switch (comparison) {
            case EQUALS -> isEqual(this.value, value);
            case EQUALS_IGNORE_CASE -> isEqualIgnoreCase(this.value, value);
            case NOT_EQUALS -> !isEqual(this.value, value);
            case NOT_EQUALS_IGNORE_CASE -> !isEqualIgnoreCase(this.value, value);
            case STARTS_WITH -> startsWith(this.value, value, false);
            case STARTS_WITH_IGNORE_CASE -> startsWith(this.value, value, true);
            case ENDS_WITH -> endsWith(this.value, value, false);
            case ENDS_WITH_IGNORE_CASE -> endsWith(this.value, value, true);
            case CONTAINS -> contains(this.value, value, false);
            case CONTAINS_IGNORE_CASE -> contains(this.value, value, true);
            case GREATER_THAN -> isOrdered(this.value, value, result -> result > 0);
            case GREATER_THAN_OR_EQUAL -> isOrdered(this.value, value, result -> result >= 0);
            case LESS_THAN -> isOrdered(this.value, value, result -> result < 0);
            case LESS_THAN_OR_EQUAL -> isOrdered(this.value, value, result -> result <= 0);
        };
    }

    private static boolean isOrdered(Object left, Object right, IntPredicate test) {
        Integer result = compareNumbers(left, right);
        if (result == null && left instanceof String leftString && right instanceof String rightString) {
            result = leftString.compareToIgnoreCase(rightString);
        }
        return result != null && test.test(result);
    }

    /**
     * Compares two values numerically when at least one is a {@link Number} and both can be converted
     * to a {@link BigDecimal}. Positive and negative infinity, which {@link BigDecimal} cannot represent,
     * order above and below every finite number.
     *
     * @return the comparison result, or {@code null} if the values are not comparable as numbers
     */
    private static Integer compareNumbers(Object left, Object right) {
        if (!(left instanceof Number) && !(right instanceof Number)) {
            return null;
        }
        if (isInfinite(left) || isInfinite(right)) {
            Double leftDouble = toDouble(left);
            Double rightDouble = toDouble(right);
            return leftDouble != null && rightDouble != null ? Double.compare(leftDouble, rightDouble) : null;
        }
        BigDecimal leftNumber = toBigDecimal(left);
        BigDecimal rightNumber = toBigDecimal(right);
        return leftNumber != null && rightNumber != null ? leftNumber.compareTo(rightNumber) : null;
    }

    private static boolean isInfinite(Object value) {
        return (value instanceof Double d && d.isInfinite()) || (value instanceof Float f && f.isInfinite());
    }

    /**
     * Converts a value to a {@code double} for comparison against infinity.
     *
     * @return the converted value, or {@code null} if {@code value} is not numeric or is NaN
     */
    private static Double toDouble(Object value) {
        if (isInfinite(value)) {
            return ((Number) value).doubleValue();
        }
        BigDecimal number = toBigDecimal(value);
        return number != null ? number.doubleValue() : null;
    }

    private static boolean startsWith(Object left, Object right, boolean ignoreCase) {
        return left instanceof String string && right instanceof String prefix
                && string.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
    }

    private static boolean contains(Object left, Object right, boolean ignoreCase) {
        if (right instanceof Map<?, ?>) {
            throw new IllegalArgumentException(
                    "CONTAINS does not accept an object to search for; reference its keys with JSON Pointers instead");
        }
        if (left instanceof Collection<?> collection) {
            return collection.stream()
                    .anyMatch(element -> ignoreCase ? isEqualIgnoreCase(element, right) : isEqual(element, right));
        }
        if (!(left instanceof String string) || !(right instanceof String substring)) {
            return false;
        }
        if (!ignoreCase) {
            return string.contains(substring);
        }
        for (int offset = 0; offset <= string.length() - substring.length(); offset++) {
            if (string.regionMatches(true, offset, substring, 0, substring.length())) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWith(Object left, Object right, boolean ignoreCase) {
        return left instanceof String string && right instanceof String suffix
                && string.regionMatches(ignoreCase, string.length() - suffix.length(), suffix, 0, suffix.length());
    }

    private static boolean isEqualIgnoreCase(Object left, Object right) {
        if (left instanceof String leftString && right instanceof String rightString) {
            return leftString.equalsIgnoreCase(rightString);
        }
        return isEqual(left, right);
    }

    private static boolean isEqual(Object left, Object right) {
        Integer result = compareNumbers(left, right);
        return result != null ? result == 0 : Objects.equals(left, right);
    }

    /**
     * Converts a number or numeric string to a {@link BigDecimal}.
     *
     * @return the converted value, or {@code null} if {@code value} is not numeric or is NaN/infinite
     */
    private static BigDecimal toBigDecimal(Object value) {
        try {
            return switch (value) {
                case BigDecimal bigDecimal -> bigDecimal;
                case BigInteger bigInteger -> new BigDecimal(bigInteger);
                // toString avoids binary floating-point noise, e.g. 0.1f -> 0.1 rather than 0.100000001...
                case Float f -> new BigDecimal(f.toString());
                case Double d -> new BigDecimal(d.toString());
                case Number number -> BigDecimal.valueOf(number.longValue());
                case String string -> new BigDecimal(string.trim());
                case null, default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
