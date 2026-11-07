package com.nineseed.alchesay.tracker.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

class CapturedValueTest {

    @Test
    void testBasicPathingWithNullValue() {
        CapturedValue capturedValue;
        final String nonNullMessage = "marked non-null";

        try {
            capturedValue = new CapturedValue("foo", null);
            fail("expected NPE");
        } catch(NullPointerException e) {
            assertThat(e.getMessage()).contains(nonNullMessage);
        }

        try {
            capturedValue = new CapturedValue(null, "fooValue");
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
}
