package com.nineseed.alchesay.tracker.rules;

import com.nineseed.alchesay.tracker.MultiThreadHelper;
import com.nineseed.alchesay.tracker.MultiThreadHelper.NamedRunnable;
import com.nineseed.alchesay.tracker.event.ValueComparison;
import com.nineseed.alchesay.tracker.event.ValueReference;
import com.nineseed.alchesay.tracker.workflow.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleTests {

    private JsonNode order1;
    private JsonNode order2;
    private JsonNode order3;
    private JsonNode inventory1;
    private ValueReference statusRef;
    private ValueReference orderIdRef;
    private Context context;
    private ValueReference eventRef;

    @BeforeEach
    void setUp() throws FileNotFoundException {
        String jsonFile = RuleTests.class.getClassLoader().getResource("orders.json").getFile();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new FileReader(jsonFile));
        order1 = rootNode.at("/order1");
        order2 = rootNode.at("/order2");
        order3 = rootNode.at("/order3");
        inventory1 = rootNode.at("/inventory1");
        statusRef = new ValueReference("status", "/payload/status");
        orderIdRef = new ValueReference("orderId", "/payload/order_id");
        eventRef = new ValueReference("event", "/payload/event");
        context = new Context();
        context.putValue(orderIdRef.capture(order1));
    }

    @Test
    void matchOrder1() {
        assertThat(order1).isNotEqualTo(MissingNode.getInstance());
        MatchingRule newOrderRule = new MatchingRule(statusRef, ValueComparison.EQUALS, "new");
        assertThat(newOrderRule.test(order1, context)).isTrue();
        assertThat(newOrderRule.test(order2, context)).isFalse();
        assertThat(newOrderRule.test(order3, context)).isFalse();
    }

    @Test
    void matchOrder2() {
        assertThat(order2).isNotEqualTo(MissingNode.getInstance());
        MatchingRule inProgressRule = new MatchingRule(statusRef, ValueComparison.EQUALS, "in-progress" );
        assertThat(inProgressRule.test(order1, context)).isFalse();
        assertThat(inProgressRule.test(order2, context)).isTrue();
        assertThat(inProgressRule.test(order3, context)).isFalse();
    }

    @Test
    void matchOrder3() {
        assertThat(order3).isNotEqualTo(MissingNode.getInstance());
        MatchingRule completedRule = new MatchingRule(statusRef, ValueComparison.EQUALS, "complete");
        assertThat(completedRule.test(order1, context)).isFalse();
        assertThat(completedRule.test(order2, context)).isFalse();
        assertThat(completedRule.test(order3, context)).isTrue();
    }

    @Test
    void testNullValues() {
        assertThat(order1).isNotEqualTo(MissingNode.getInstance());
        MatchingRule newOrderRule = new MatchingRule(statusRef, ValueComparison.EQUALS, null);
        assertThat(newOrderRule.test(order1, context)).isFalse();
    }

    @Test
    void testMultipleValues1() {
        Rule inProgressRule = new LogicalRule(List.of(
                new MatchingRule(statusRef, ValueComparison.EQUALS, "in-progress"),
                // pulls the orderId from the context
                new MatchingRule(orderIdRef, ValueComparison.EQUALS, "${orderId}")
        ), LogicalOperator.AND);
        assertThat(inProgressRule.test(order1, context)).isFalse();
        assertThat(inProgressRule.test(order2, context)).isTrue();
        assertThat(inProgressRule.test(order3, context)).isFalse();
    }

    @Test
    void testMultipleValues2() {
        Rule completeRule = new LogicalRule(List.of(
                new MatchingRule(statusRef, ValueComparison.EQUALS, "complete"),
                // pulls the orderId from the context
                new MatchingRule(orderIdRef, ValueComparison.EQUALS, "${orderId}")
        ), LogicalOperator.AND);
        assertThat(completeRule.test(order1, context)).isFalse();
        assertThat(completeRule.test(order2, context)).isFalse();
        assertThat(completeRule.test(order3, context)).isTrue();
    }

    @Test
    void testMultipleValues3() {
        Rule inventoryRule = new LogicalRule(List.of(
                new MatchingRule(eventRef, ValueComparison.EQUALS, "inventory_confirmed"),
                // pulls the orderId from the context
                new MatchingRule(orderIdRef, ValueComparison.EQUALS, "ORD-${orderId}")
        ), LogicalOperator.AND);
        assertThat(inventoryRule.test(order1, context)).isFalse();
        assertThat(inventoryRule.test(order2, context)).isFalse();
        assertThat(inventoryRule.test(order3, context)).isFalse();
        assertThat(inventoryRule.test(inventory1, context)).isTrue();
    }

    @Test
    void testEscapedValues() {
        ValueReference notesRef = new ValueReference("notes", "/payload/notes");
        Rule inventoryRule = new LogicalRule(List.of(
                new MatchingRule(eventRef, ValueComparison.EQUALS, "inventory_confirmed"),
                // pulls the orderId from the context
                new MatchingRule(orderIdRef, ValueComparison.EQUALS, "ORD-${orderId}"),
                new MatchingRule(notesRef, ValueComparison.CONTAINS, "\\\\$\\{count\\}\\")
        ), LogicalOperator.AND);
        assertThat(inventoryRule.test(order1, context)).isFalse();
        assertThat(inventoryRule.test(order2, context)).isFalse();
        assertThat(inventoryRule.test(order3, context)).isFalse();
        assertThat(inventoryRule.test(inventory1, context)).isTrue();
    }

    @Test
    void testMultipleThreads() throws InterruptedException {
        ValueReference notesRef = new ValueReference("notes", "/payload/notes");
        Rule inventoryRule = new LogicalRule(List.of(
                new MatchingRule(eventRef, ValueComparison.EQUALS, "inventory_confirmed"),
                new MatchingRule(orderIdRef, ValueComparison.EQUALS, "ORD-${orderId}")
        ), LogicalOperator.AND);

        Rule inProgressRule = new LogicalRule(List.of(
                new MatchingRule(statusRef, ValueComparison.EQUALS, "in-progress"),
                // pulls the orderId from the context
                new MatchingRule(orderIdRef, ValueComparison.EQUALS, "${orderId}")
        ), LogicalOperator.AND);

        MultiThreadHelper helper = new MultiThreadHelper(List.of(
                new NamedRunnable("test inventory rule", () -> {
                    assertThat(inventoryRule.test(order1, context)).isFalse();
                    assertThat(inventoryRule.test(order2, context)).isFalse();
                    assertThat(inventoryRule.test(order3, context)).isFalse();
                    assertThat(inventoryRule.test(inventory1, context)).isTrue();
                }),
                new NamedRunnable("test order2 rule", () -> {
                    assertThat(inProgressRule.test(order1, context)).isFalse();
                    assertThat(inProgressRule.test(order2, context)).isTrue();
                    assertThat(inProgressRule.test(order3, context)).isFalse();
                })
        ), List.of(50, 50), Duration.ofMillis(500));

        helper.test();
    }
}