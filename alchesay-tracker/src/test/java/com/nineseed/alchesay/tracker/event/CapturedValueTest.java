package com.nineseed.alchesay.tracker.event;

import org.junit.jupiter.api.Test;

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
}
