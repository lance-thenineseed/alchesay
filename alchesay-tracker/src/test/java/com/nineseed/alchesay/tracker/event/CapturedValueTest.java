package com.nineseed.alchesay.tracker.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.nineseed.alchesay.tracker.event.CapturedValue.Comparison.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

class CapturedValueTest {

    @SuppressWarnings({"DataFlowIssue"})
    @Test
    void testBasicPathingWithNullValue() {
        final String nonNullMessage = "marked non-null";

        try {
            new CapturedValue("foo", null);
            fail("expected NPE");
        } catch(NullPointerException e) {
            assertThat(e.getMessage()).contains(nonNullMessage);
        }

        try {
            new CapturedValue(null, "fooValue");
            fail("expected NPE");
        } catch(NullPointerException e) {
            assertThat(e.getMessage()).contains(nonNullMessage);
        }
    }

    @Test
    void testEquals() {
        CapturedValue capturedValue1 = new CapturedValue("foo", "fooValue");
        CapturedValue capturedValue2 = new CapturedValue("foo", "fooValue");
        CapturedValue differentName = new CapturedValue("bar", "fooValue");
        CapturedValue differentValue = new CapturedValue("foo", "barValue");
        assertThat(capturedValue1).isEqualTo(capturedValue2);
        assertThat(capturedValue1.hashCode()).isEqualTo(capturedValue2.hashCode());
        assertThat(capturedValue1).isNotEqualTo(differentName);
        assertThat(capturedValue1).isNotEqualTo(differentValue);

        CapturedValue missing = new CapturedValue("foo", CapturedValue.MISSING_VALUE);
        CapturedValue nullValue = new CapturedValue("foo", CapturedValue.NULL_VALUE);
        assertThat(missing).isEqualTo(new CapturedValue("foo", CapturedValue.MISSING_VALUE));
        assertThat(missing).isNotEqualTo(nullValue);
        assertThat(missing).isNotEqualTo(new CapturedValue("foo", ""));
    }

    @Test
    void testCustomToString() {
        assertThat(CapturedValue.MISSING_VALUE.toString()).isEqualTo("MISSING_VALUE");
        assertThat(CapturedValue.NULL_VALUE.toString()).isEqualTo("NULL_VALUE");
    }

    @Test
    void testBasicNumericComparisonEquality() {
        CapturedValue capturedValue = new CapturedValue("bar", 42);
        assertThat(capturedValue.compare(EQUALS, 42)).isTrue();
        assertThat(capturedValue.compare(EQUALS, 42L)).isTrue();
        assertThat(capturedValue.compare(EQUALS, 42.0f)).isTrue();
        assertThat(capturedValue.compare(EQUALS, 42.0)).isTrue();
        assertThat(capturedValue.compare(EQUALS, 0x2A)).isTrue();
        assertThat(capturedValue.compare(EQUALS, "42")).isTrue();
        assertThat(capturedValue.compare(EQUALS, "42.0")).isTrue();
        assertThat(capturedValue.compare(EQUALS, 42.1)).isFalse();
        assertThat(capturedValue.compare(EQUALS, "42.1")).isFalse();
        assertThat(capturedValue.compare(EQUALS, "-42.1")).isFalse();
        assertThat(capturedValue.compare(EQUALS, "flugelhorn")).isFalse();
        assertThat(capturedValue.compare(EQUALS, List.of(42))).isFalse();
        assertThat(capturedValue.compare(EQUALS, Map.of("forty-two", 42))).isFalse();

        capturedValue = new CapturedValue("bar", 42);
        assertThat(capturedValue.compare(NOT_EQUALS, 42)).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, 42L)).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, 42.0f)).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, 42.0)).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, 0x2A)).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, "42")).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, "42.0")).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS, 42.1)).isTrue();
        assertThat(capturedValue.compare(NOT_EQUALS, "42.1")).isTrue();
        assertThat(capturedValue.compare(NOT_EQUALS, "-42.1")).isTrue();
        assertThat(capturedValue.compare(NOT_EQUALS, "flugelhorn")).isTrue();
        assertThat(capturedValue.compare(NOT_EQUALS, List.of(42))).isTrue();
        assertThat(capturedValue.compare(NOT_EQUALS, Map.of("forty-two", 42))).isTrue();
    }

    @Test
    void testBasicNumericComparisonOrdering() {
        CapturedValue capturedValue = new CapturedValue("bar", 42.1);
        assertThat(capturedValue.compare(GREATER_THAN, 40)).isTrue();
        assertThat(capturedValue.compare(GREATER_THAN, 43.0)).isFalse();
        assertThat(capturedValue.compare(GREATER_THAN_OR_EQUAL, 42.0)).isTrue();
        assertThat(capturedValue.compare(GREATER_THAN_OR_EQUAL, 45.0)).isFalse();
        assertThat(capturedValue.compare(GREATER_THAN_OR_EQUAL, 42.1)).isTrue();

        assertThat(capturedValue.compare(LESS_THAN, 40)).isFalse();
        assertThat(capturedValue.compare(LESS_THAN, 43.0)).isTrue();
        assertThat(capturedValue.compare(LESS_THAN_OR_EQUAL, 42.0)).isFalse();
        assertThat(capturedValue.compare(LESS_THAN_OR_EQUAL, 45.0)).isTrue();
        assertThat(capturedValue.compare(LESS_THAN_OR_EQUAL, 42.1)).isTrue();
    }

    @Test
    void testStringComparison() {
        CapturedValue capturedValue = new CapturedValue("foo", "fooValue");
        assertThat(capturedValue.compare(EQUALS, "fooValue")).isTrue();
        assertThat(capturedValue.compare(EQUALS, "barValue")).isFalse();
        assertThat(capturedValue.compare(EQUALS_IGNORE_CASE, "FOOvalue")).isTrue();
        assertThat(capturedValue.compare(EQUALS_IGNORE_CASE, "BARvalue")).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS_IGNORE_CASE, "Foovalue")).isFalse();
        assertThat(capturedValue.compare(NOT_EQUALS_IGNORE_CASE, "Foovalue2")).isTrue();

        assertThat(capturedValue.compare(STARTS_WITH, "foo")).isTrue();
        assertThat(capturedValue.compare(STARTS_WITH, "fooValue")).isTrue();
        assertThat(capturedValue.compare(STARTS_WITH, "fool")).isFalse();

        assertThat(capturedValue.compare(ENDS_WITH, "Value")).isTrue();
        assertThat(capturedValue.compare(ENDS_WITH, "fooValue")).isTrue();
        assertThat(capturedValue.compare(ENDS_WITH, "lxe")).isFalse();

        assertThat(capturedValue.compare(STARTS_WITH_IGNORE_CASE, "foo")).isTrue();
        assertThat(capturedValue.compare(STARTS_WITH_IGNORE_CASE, "FOOValue")).isTrue();
        assertThat(capturedValue.compare(STARTS_WITH_IGNORE_CASE, "Fool")).isFalse();

        assertThat(capturedValue.compare(ENDS_WITH_IGNORE_CASE, "value")).isTrue();
        assertThat(capturedValue.compare(ENDS_WITH_IGNORE_CASE, "FOOValue")).isTrue();
        assertThat(capturedValue.compare(ENDS_WITH_IGNORE_CASE, "lxE")).isFalse();
    }

    @Test
    public void testContains() {
        CapturedValue capturedValue = new CapturedValue("foo", "fooValue");

        assertThat(capturedValue.compare(CONTAINS, "foo")).isTrue();
        assertThat(capturedValue.compare(CONTAINS, "fooValue")).isTrue();
        assertThat(capturedValue.compare(CONTAINS, "al")).isTrue();
        assertThat(capturedValue.compare(CONTAINS, "x")).isFalse();
        assertThat(capturedValue.compare(CONTAINS, " fooValue")).isFalse();
        assertThat(capturedValue.compare(CONTAINS, " ")).isFalse();
        assertThat(capturedValue.compare(CONTAINS, "")).isTrue();

        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, "Foo")).isTrue();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, "fOoValue")).isTrue();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, "aL")).isTrue();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, "X")).isFalse();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, " FooValue")).isFalse();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, " ")).isFalse();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, "")).isTrue();

        capturedValue = new CapturedValue("foo", "42");
        assertThat(capturedValue.compare(CONTAINS, 42)).isFalse();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, 42)).isFalse();

        capturedValue = new CapturedValue("foo", List.of("a", "b", 42));
        assertThat(capturedValue.compare(CONTAINS, 42)).isTrue();
        assertThat(capturedValue.compare(CONTAINS, 42L)).isTrue();
        assertThat(capturedValue.compare(CONTAINS, 42.0)).isTrue();
        assertThat(capturedValue.compare(CONTAINS, "42")).isTrue();
        assertThat(capturedValue.compare(CONTAINS, "b")).isTrue();
        assertThat(capturedValue.compare(CONTAINS_IGNORE_CASE, "A")).isTrue();
        assertThat(capturedValue.compare(CONTAINS, "B")).isFalse();
        assertThat(capturedValue.compare(CONTAINS, "")).isFalse();

        CapturedValue capturedMap = new CapturedValue("map", Map.of("blue", "green"));
        assertThat(capturedMap.compare(CONTAINS, "blue")).isFalse();
        assertThat(capturedMap.compare(CONTAINS, "green")).isFalse();
        assertThatThrownBy(() -> capturedMap.compare(CONTAINS, Map.of("blue", "green")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CapturedValue("joe", 471).compare(CONTAINS, Map.of("value", "471")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testStringOrdering() {
        // Ordering is always case-insensitive
        CapturedValue capturedValue = new CapturedValue("foo", "limestone");
        assertThat(capturedValue.compare(GREATER_THAN, "limestone")).isFalse();
        assertThat(capturedValue.compare(GREATER_THAN, "aardvark")).isTrue();
        assertThat(capturedValue.compare(GREATER_THAN, "Aardvark")).isTrue();
        assertThat(capturedValue.compare(GREATER_THAN, "zebra")).isFalse();
        assertThat(capturedValue.compare(GREATER_THAN_OR_EQUAL, "limestone")).isTrue();

        assertThat(capturedValue.compare(LESS_THAN, "limestone")).isFalse();
        assertThat(capturedValue.compare(LESS_THAN, "aardvark")).isFalse();
        assertThat(capturedValue.compare(LESS_THAN, "Aardvark")).isFalse();
        assertThat(capturedValue.compare(LESS_THAN, "zebra")).isTrue();
        assertThat(capturedValue.compare(LESS_THAN_OR_EQUAL, "limestone")).isTrue();
    }

    @Test
    void testMissingValueComparisons() {
        CapturedValue missing = new CapturedValue("status", CapturedValue.MISSING_VALUE);

        // A rule can test for an absent field by comparing against the sentinel
        assertThat(missing.compare(EQUALS, CapturedValue.MISSING_VALUE)).isTrue();
        assertThat(missing.compare(EQUALS, CapturedValue.NULL_VALUE)).isFalse();
        assertThat(missing.compare(EQUALS, "SHIPPED")).isFalse();
        assertThat(missing.compare(EQUALS, "")).isFalse();

        assertThat(missing.compare(NOT_EQUALS, "SHIPPED")).isTrue();
        assertThat(missing.compare(NOT_EQUALS_IGNORE_CASE, "SHIPPED")).isTrue();
        assertThat(missing.compare(NOT_EQUALS, CapturedValue.MISSING_VALUE)).isFalse();

        assertThat(missing.compare(GREATER_THAN, 0)).isFalse();
        assertThat(missing.compare(GREATER_THAN_OR_EQUAL, 0)).isFalse();
        assertThat(missing.compare(LESS_THAN, 0)).isFalse();
        assertThat(missing.compare(LESS_THAN_OR_EQUAL, 0)).isFalse();
        assertThat(missing.compare(LESS_THAN, "zzz")).isFalse();

        assertThat(missing.compare(CONTAINS, "")).isFalse();
        assertThat(missing.compare(STARTS_WITH, "")).isFalse();
        assertThat(missing.compare(ENDS_WITH, "")).isFalse();
    }

    @Test
    void testNullValueComparisons() {
        CapturedValue nullValue = new CapturedValue("status", CapturedValue.NULL_VALUE);

        // A rule can test for an explicit JSON null by comparing against the sentinel
        assertThat(nullValue.compare(EQUALS, CapturedValue.NULL_VALUE)).isTrue();
        assertThat(nullValue.compare(EQUALS, CapturedValue.MISSING_VALUE)).isFalse();
        assertThat(nullValue.compare(EQUALS, "null")).isFalse();
        assertThat(nullValue.compare(EQUALS, 0)).isFalse();

        assertThat(nullValue.compare(NOT_EQUALS, "SHIPPED")).isTrue();
        assertThat(nullValue.compare(NOT_EQUALS, CapturedValue.NULL_VALUE)).isFalse();

        assertThat(nullValue.compare(GREATER_THAN, 0)).isFalse();
        assertThat(nullValue.compare(LESS_THAN, 0)).isFalse();
        assertThat(nullValue.compare(CONTAINS, "")).isFalse();
        assertThat(nullValue.compare(STARTS_WITH, "")).isFalse();
    }

    @Test
    void testOtherCapturedNumberTypes() {
        // readTree produces Long and BigInteger for large integers
        CapturedValue longValue = new CapturedValue("long", 3_000_000_000L);
        assertThat(longValue.compare(EQUALS, "3000000000")).isTrue();
        assertThat(longValue.compare(EQUALS, 3_000_000_000.0)).isTrue();
        assertThat(longValue.compare(GREATER_THAN, Integer.MAX_VALUE)).isTrue();

        CapturedValue bigInteger = new CapturedValue("bigInt", new BigInteger("12345678901234567890"));
        assertThat(bigInteger.compare(GREATER_THAN, Long.MAX_VALUE)).isTrue();
        assertThat(bigInteger.compare(EQUALS, "12345678901234567890")).isTrue();
        assertThat(bigInteger.compare(EQUALS, "12345678901234567891")).isFalse();

        CapturedValue bigDecimal = new CapturedValue("bigDecimal", new BigDecimal("42.10"));
        assertThat(bigDecimal.compare(EQUALS, 42.1)).isTrue();
        assertThat(bigDecimal.compare(EQUALS, "42.1000")).isTrue();
    }

    @Test
    void testFloatingPointPrecision() {
        // Floats and doubles are compared by their decimal string form, so 0.1f equals 0.1
        // even though their binary representations differ
        CapturedValue floatValue = new CapturedValue("float", 0.1f);
        assertThat(floatValue.compare(EQUALS, 0.1)).isTrue();
        assertThat(floatValue.compare(EQUALS, "0.1")).isTrue();
        assertThat(floatValue.compare(GREATER_THAN, 0.1)).isFalse();

        // 0.1 + 0.2 is 0.30000000000000004 as a double, so it is not equal to 0.3
        CapturedValue sum = new CapturedValue("sum", 0.1 + 0.2);
        assertThat(sum.compare(EQUALS, 0.3)).isFalse();
        assertThat(sum.compare(GREATER_THAN, 0.3)).isTrue();
    }

    @Test
    void testNegativesAndZero() {
        CapturedValue negative = new CapturedValue("negative", -1);
        assertThat(negative.compare(LESS_THAN, 0)).isTrue();
        assertThat(negative.compare(GREATER_THAN, -2)).isTrue();
        assertThat(negative.compare(EQUALS, "-1")).isTrue();
        assertThat(negative.compare(EQUALS, "-1.0")).isTrue();
        assertThat(negative.compare(EQUALS, 1)).isFalse();

        CapturedValue negativeZero = new CapturedValue("negativeZero", -0.0);
        assertThat(negativeZero.compare(EQUALS, 0)).isTrue();
        assertThat(negativeZero.compare(EQUALS, 0.0)).isTrue();
        assertThat(negativeZero.compare(LESS_THAN, 0)).isFalse();
    }

    @Test
    void testInfinityAndNaN() {
        // Not valid in standard JSON, but possible from Java callers or lenient parsers.
        CapturedValue infinity = new CapturedValue("infinity", Double.POSITIVE_INFINITY);
        assertThat(infinity.compare(GREATER_THAN, 5)).isTrue();
        assertThat(infinity.compare(LESS_THAN, 5)).isFalse();
        assertThat(infinity.compare(EQUALS, Double.POSITIVE_INFINITY)).isTrue();

        CapturedValue negativeInfinity = new CapturedValue("negativeInfinity", Double.NEGATIVE_INFINITY);
        assertThat(negativeInfinity.compare(LESS_THAN, -5)).isTrue();
        assertThat(negativeInfinity.compare(LESS_THAN, "-5")).isTrue();
        assertThat(negativeInfinity.compare(LESS_THAN, -Double.MAX_VALUE)).isTrue();
        assertThat(negativeInfinity.compare(LESS_THAN, Double.POSITIVE_INFINITY)).isTrue();
        assertThat(negativeInfinity.compare(GREATER_THAN, -5)).isFalse();
        assertThat(negativeInfinity.compare(EQUALS, Double.NEGATIVE_INFINITY)).isTrue();
        assertThat(negativeInfinity.compare(EQUALS, Float.NEGATIVE_INFINITY)).isTrue();
        assertThat(negativeInfinity.compare(EQUALS, Double.POSITIVE_INFINITY)).isFalse();
        assertThat(negativeInfinity.compare(LESS_THAN_OR_EQUAL, Double.NEGATIVE_INFINITY)).isTrue();
        assertThat(negativeInfinity.compare(LESS_THAN, Double.NEGATIVE_INFINITY)).isFalse();

        // Finite captured values against negative infinity
        CapturedValue five = new CapturedValue("five", 5);
        assertThat(five.compare(GREATER_THAN, Double.NEGATIVE_INFINITY)).isTrue();
        assertThat(five.compare(LESS_THAN, Double.NEGATIVE_INFINITY)).isFalse();
        assertThat(new CapturedValue("five", "5").compare(GREATER_THAN, Double.NEGATIVE_INFINITY)).isTrue();

        // NaN falls back to Double.equals, which treats NaN as equal to itself
        CapturedValue nan = new CapturedValue("nan", Double.NaN);
        assertThat(nan.compare(EQUALS, Double.NaN)).isTrue();
        assertThat(nan.compare(EQUALS, 0)).isFalse();
        assertThat(nan.compare(GREATER_THAN, 0)).isFalse();
        assertThat(nan.compare(LESS_THAN, 0)).isFalse();
    }

    @Test
    void testNumericStringOnCapturedSide() {
        CapturedValue numericString = new CapturedValue("count", "42");
        assertThat(numericString.compare(EQUALS, 42)).isTrue();
        assertThat(numericString.compare(EQUALS, 42.0)).isTrue();
        assertThat(numericString.compare(GREATER_THAN, 40)).isTrue();
        assertThat(numericString.compare(LESS_THAN, 40)).isFalse();

        // Two strings are never compared as numbers
        assertThat(numericString.compare(EQUALS, "42.0")).isFalse();
        assertThat(numericString.compare(EQUALS, "42")).isTrue();
    }

    @Test
    void testNumericStringParsing() {
        CapturedValue fortyTwo = new CapturedValue("count", 42);
        assertThat(fortyTwo.compare(EQUALS, " 42 ")).isTrue();
        assertThat(fortyTwo.compare(EQUALS, "4.2e1")).isTrue();
        assertThat(fortyTwo.compare(EQUALS, "+42")).isTrue();

        // Not parsed as numbers
        assertThat(fortyTwo.compare(EQUALS, "0x2A")).isFalse();
        assertThat(fortyTwo.compare(EQUALS, "42abc")).isFalse();
        assertThat(fortyTwo.compare(EQUALS, "42,0")).isFalse();
        assertThat(fortyTwo.compare(EQUALS, "")).isFalse();

        CapturedValue thousand = new CapturedValue("count", 1000);
        assertThat(thousand.compare(EQUALS, "1e3")).isTrue();
        assertThat(thousand.compare(EQUALS, "1,000")).isFalse();
    }
}
