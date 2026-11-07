package com.nineseed.alchesay.tracker.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Fail.fail;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

class ValueReferenceTest {

    private HashMap<String, Object> event;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        event = new HashMap<>();
        event.put("foo", "fooValue");
        event.put("bar", "barValue");
        event.put("baz", Map.of("qux", "quxValue"));
        event.put("theBool", true);
        event.put("theInt", 123);
        event.put("theFloat", 123.456f);
        event.put("theDouble", 123.45);

        objectMapper = new ObjectMapper();
    }

    @Test
    void testBasicPathing() {
        CapturedValue capturedValue;
        ValueReference valueReference = new ValueReference("foo", "/foo");
        JsonNode node = objectMapper.valueToTree(event);
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value().toString()).isEqualTo("fooValue");
        assertThat(capturedValue.value()).isInstanceOf(String.class);

        valueReference = new ValueReference("qux", "/baz/qux");
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isInstanceOf(String.class);
        assertThat(capturedValue.value()).isEqualTo("quxValue");
    }

    @Test
    void testNullValues() {
        final String nonNullMessage = "marked non-null";
        String testName = null;
        String testPath = null;
        JsonNode testNode = null;

        try {
            ValueReference valueReference = new ValueReference("foo", "/foo");
            CapturedValue capturedValue = valueReference.capture(testNode);
            fail("expected NPE");
        } catch(NullPointerException e) {
            assertThat(e.getMessage()).contains(nonNullMessage);
        }

        try {
            ValueReference valueReference = new ValueReference("foo", testPath);
            fail("expected NPE");
        } catch(NullPointerException e) {
            assertThat(e.getMessage()).contains(nonNullMessage);
        }

        try {
            ValueReference valueReference = new ValueReference(testName, "/path");
            fail("expected NPE");
        } catch(NullPointerException e) {
            assertThat(e.getMessage()).contains(nonNullMessage);
        }
    }

    @Test
    void testEquals() {
        ValueReference valueReference1 = new ValueReference("foo", "/foo");
        ValueReference valueReference2 = new ValueReference("foo", "/foo");
        ValueReference valueReference3 = new ValueReference("bar", "/bar");
        ValueReference valueReference4 = new ValueReference("foo", "/bar");
        assertThat(valueReference1).isEqualTo(valueReference2);
        assertThat(valueReference1).isNotEqualTo(valueReference3);
        assertThat(valueReference1).isNotEqualTo(valueReference4);
        assertThat(valueReference3).isNotEqualTo(valueReference4);
        assertThat(valueReference1.hashCode()).isEqualTo(valueReference2.hashCode());
    }

    @Test
    void testBasicTypes() {
        CapturedValue capturedValue;
        ValueReference valueReference = new ValueReference("theBool", "/theBool");
        JsonNode node = objectMapper.valueToTree(event);
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isInstanceOf(Boolean.class);
        assertThat(((Boolean)capturedValue.value())).isTrue();

        valueReference = new ValueReference("theInt", "/theInt");
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isInstanceOf(Integer.class);
        assertThat(((Integer)capturedValue.value()).intValue()).isEqualTo(123);
        valueReference = new ValueReference("theFloat", "/theFloat");
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isInstanceOf(Float.class);
        assertThat(((Float)capturedValue.value()).floatValue()).isEqualTo(123.456f);
        valueReference = new ValueReference("theDouble", "/theDouble");
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isInstanceOf(Double.class);
        assertThat(((Double)capturedValue.value()).doubleValue()).isEqualTo(123.45);
    }

    @Test
    void testMissingValues() {
        ValueReference valueReference = new ValueReference("foo", "/fooXX");
        JsonNode node = objectMapper.valueToTree(event);
        CapturedValue capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isEqualTo(CapturedValue.MISSING_VALUE);
    }

    @Test
    void testJsonNullValue() {
        ValueReference valueReference = new ValueReference("foo", "/foo");
        JsonNode node = objectMapper.readTree("{ \"foo\": null }");
        CapturedValue capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isEqualTo(CapturedValue.NULL_VALUE);
    }

    @Test
    void testArrays() {
        CapturedValue capturedValue;
        event.put("theList", List.of("a", "b", "c"));
        ValueReference valueReference = new ValueReference("theList", "/theList");
        JsonNode node = objectMapper.valueToTree(event);
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value())
                .asInstanceOf(LIST)
                .containsExactly("a", "b", "c");
        valueReference = new ValueReference("theList", "/theList/1");
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value()).isInstanceOf(String.class);
        assertThat((capturedValue.value().toString())).isEqualTo("b");
    }

    @Test
    void testMaps() {
        CapturedValue capturedValue;
        event.put("theMap", Map.of("key1", "value1", "key2", "value2"));
        ValueReference valueReference = new ValueReference("theMap", "/theMap");
        JsonNode node = objectMapper.valueToTree(event);
        capturedValue = valueReference.capture(node);
        assertThat(capturedValue.value())
                .asInstanceOf(map(String.class, Object.class))
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
    }

    @Test
    void testCapturedValueEquals() {
        CapturedValue capturedValue1;
        CapturedValue capturedValue2;
        ValueReference valueReference1 = new ValueReference("foo", "/foo");
        ValueReference valueReference2 = new ValueReference("foo", "/foo");
        assertThat(valueReference1).isEqualTo(valueReference2);

        JsonNode node1 = objectMapper.valueToTree(event);

        event.put("foo", "fooValue2");
        JsonNode node2 = objectMapper.valueToTree(event);
        capturedValue1 = valueReference1.capture(node1);
        capturedValue2 = valueReference2.capture(node2);
        assertThat(capturedValue1).isNotEqualTo(capturedValue2);
        capturedValue2 = valueReference2.capture(node1);
        assertThat(capturedValue1).isEqualTo(capturedValue2);
    }

    @Test
    void testCapturedNameMatchesReference() {
        ValueReference valueReference = new ValueReference("myName", "/foo");
        CapturedValue capturedValue = valueReference.capture(objectMapper.valueToTree(event));
        assertThat(capturedValue.name()).isEqualTo("myName");
    }

    @Test
    void testNestedNullsAndMissing() {
        JsonNode node = objectMapper.readTree("{ \"list\": [1, null], \"obj\": { \"a\": null } }");

        assertThat(new ValueReference("list", "/list").capture(node).value())
                .asInstanceOf(LIST)
                .containsExactly(1, CapturedValue.NULL_VALUE);
        assertThat(new ValueReference("obj", "/obj").capture(node).value())
                .asInstanceOf(map(String.class, Object.class))
                .containsExactly(entry("a", CapturedValue.NULL_VALUE));
        assertThat(new ValueReference("outOfBounds", "/list/5").capture(node).value())
                .isEqualTo(CapturedValue.MISSING_VALUE);
        assertThat(new ValueReference("missingField", "/obj/b").capture(node).value())
                .isEqualTo(CapturedValue.MISSING_VALUE);
    }

    @Test
    void testDeepNesting() {
        JsonNode node = objectMapper.readTree("""
                { "orders": [ { "id": 1, "tags": ["x", "y"] }, { "id": 2, "tags": [] } ] }
                """);
        CapturedValue capturedValue = new ValueReference("orders", "/orders").capture(node);

        assertThat(capturedValue.value())
                .isEqualTo(List.of(
                        Map.of("id", 1, "tags", List.of("x", "y")),
                        Map.of("id", 2, "tags", List.of())));
        assertThat(new ValueReference("tag", "/orders/0/tags/1").capture(node).value())
                .isEqualTo("y");
    }

    @Test
    void testObjectFieldOrderPreserved() {
        JsonNode node = objectMapper.readTree("{ \"obj\": { \"b\": 1, \"a\": 2, \"c\": 3 } }");
        assertThat(new ValueReference("obj", "/obj").capture(node).value())
                .asInstanceOf(map(String.class, Object.class))
                .containsExactly(entry("b", 1), entry("a", 2), entry("c", 3));
    }

    @Test
    void testNumberTypesFromParsedJson() {
        JsonNode node = objectMapper.readTree("""
                { "int": 123, "long": 3000000000, "bigInt": 12345678901234567890, "decimal": 123.456 }
                """);

        assertThat(new ValueReference("int", "/int").capture(node).value()).isEqualTo(123);
        assertThat(new ValueReference("long", "/long").capture(node).value()).isEqualTo(3000000000L);
        assertThat(new ValueReference("bigInt", "/bigInt").capture(node).value())
                .isEqualTo(new BigInteger("12345678901234567890"));
        assertThat(new ValueReference("decimal", "/decimal").capture(node).value()).isEqualTo(123.456);
    }

    @Test
    void testEmptyPathReturnsRoot() {
        JsonNode objectRoot = objectMapper.readTree("{ \"a\": 1 }");
        assertThat(new ValueReference("root", "").capture(objectRoot).value())
                .isEqualTo(Map.of("a", 1));

        JsonNode arrayRoot = objectMapper.readTree("[1, 2]");
        assertThat(new ValueReference("root", "").capture(arrayRoot).value())
                .isEqualTo(List.of(1, 2));
    }

    @Test
    void testEscapedPointerKeys() {
        JsonNode node = objectMapper.readTree("{ \"a/b\": \"slash\", \"a~b\": \"tilde\" }");
        assertThat(new ValueReference("slash", "/a~1b").capture(node).value()).isEqualTo("slash");
        assertThat(new ValueReference("tilde", "/a~0b").capture(node).value()).isEqualTo("tilde");
    }

    @Test
    void testInvalidPathRejectedAtConstruction() {
        assertThatThrownBy(() -> new ValueReference("foo", "foo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEmptyAndFalseValues() {
        JsonNode node = objectMapper.readTree("""
                { "false": false, "emptyString": "", "emptyList": [], "emptyObject": {} }
                """);

        assertThat(new ValueReference("false", "/false").capture(node).value()).isEqualTo(false);
        assertThat(new ValueReference("emptyString", "/emptyString").capture(node).value()).isEqualTo("");
        assertThat(new ValueReference("emptyList", "/emptyList").capture(node).value())
                .asInstanceOf(LIST)
                .isEmpty();
        assertThat(new ValueReference("emptyObject", "/emptyObject").capture(node).value())
                .asInstanceOf(map(String.class, Object.class))
                .isEmpty();
    }

    @Test
    void testCapturedCollectionsAreUnmodifiable() {
        JsonNode node = objectMapper.readTree("{ \"list\": [ { \"a\": 1 } ], \"obj\": { \"list\": [1] } }");

        List<?> list = (List<?>) new ValueReference("list", "/list").capture(node).value();
        assertThatThrownBy(() -> list.add(null)).isInstanceOf(UnsupportedOperationException.class);
        Map<?, ?> nestedMap = (Map<?, ?>) list.get(0);
        assertThatThrownBy(nestedMap::clear).isInstanceOf(UnsupportedOperationException.class);

        Map<?, ?> map = (Map<?, ?>) new ValueReference("obj", "/obj").capture(node).value();
        assertThatThrownBy(map::clear).isInstanceOf(UnsupportedOperationException.class);
        List<?> nestedList = (List<?>) map.get("list");
        assertThatThrownBy(nestedList::clear).isInstanceOf(UnsupportedOperationException.class);
    }
}