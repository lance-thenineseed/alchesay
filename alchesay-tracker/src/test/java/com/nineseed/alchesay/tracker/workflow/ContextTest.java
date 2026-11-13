package com.nineseed.alchesay.tracker.workflow;

import com.nineseed.alchesay.tracker.event.CapturedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextTest {

    public static final int NUM_VALUES = 1000;

    @BeforeEach
    void setUp() {
    }

    @Test
    void context() {
        Context context = new Context();
        CapturedValue value1 = new CapturedValue("id", "87654");
        context.putValue(value1);
        CapturedValue value2 = new CapturedValue("status", "in-process");
        context.putValue(value2);
        assertThat(context.getValues()).containsExactly(value1, value2);
        CapturedValue value3 = new CapturedValue("status", "completed");
        context.putValue(value3);
        assertThat(context.getValues()).containsExactly(value1, value3);

        assertThat(context.find("status")).isEqualTo(value3);
        assertThat(context.find("missing").value()).isEqualTo(CapturedValue.MISSING_VALUE);
    }

    @Test
    void testOrder() {
        Context context = new Context();
        CapturedValue value1 = new CapturedValue("z", "87654");
        CapturedValue value2 = new CapturedValue("m", 467.03);
        CapturedValue value3 = new CapturedValue("b", true);

        context.putAll(List.of(value1, value2, value3));
        assertThat(context.getValues()).containsExactly(value3, value2, value1);
    }

    @Test
    void testOrderIgnoresCase() {
        Context context = new Context();
        CapturedValue lowerB = new CapturedValue("b", 1);
        CapturedValue upperA = new CapturedValue("A", 2);
        CapturedValue upperC = new CapturedValue("C", 3);

        context.putAll(List.of(lowerB, upperA, upperC));
        assertThat(context.getValues()).containsExactly(upperA, lowerB, upperC);
    }

    @Test
    void testNamesDifferingOnlyInCaseAreDistinct() {
        Context context = new Context();
        CapturedValue lower = new CapturedValue("status", "lower");
        CapturedValue upper = new CapturedValue("Status", "upper");

        context.putAll(List.of(lower, upper));
        assertThat(context.getValues()).containsExactlyInAnyOrder(lower, upper);
        assertThat(context.find("status")).isEqualTo(lower);
        assertThat(context.find("Status")).isEqualTo(upper);
        assertThat(context.find("STATUS").value()).isEqualTo(CapturedValue.MISSING_VALUE);
    }

    @Test
    void testEmptyContext() {
        Context context = new Context();
        assertThat(context.getValues()).isEmpty();
        assertThat(context.find("x").value()).isEqualTo(CapturedValue.MISSING_VALUE);
    }

    @Test
    void testFindMissingKeepsNameAndDoesNotAdd() {
        Context context = new Context();
        CapturedValue id = new CapturedValue("id", 42);
        context.putValue(id);

        CapturedValue missing = context.find("x");
        assertThat(missing.name()).isEqualTo("x");
        assertThat(missing.value()).isEqualTo(CapturedValue.MISSING_VALUE);
        assertThat(context.getValues()).containsExactly(id);
    }

    @Test
    void testNullArguments() {
        Context context = new Context();
        assertThatThrownBy(() -> context.putValue(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> context.putAll(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> context.find(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testPutAllWithNullElementAddsNothing() {
        Context context = new Context();
        CapturedValue a = new CapturedValue("a", 1);
        CapturedValue b = new CapturedValue("b", 2);

        assertThatThrownBy(() -> context.putAll(Arrays.asList(a, null, b)))
                .isInstanceOf(NullPointerException.class);
        assertThat(context.getValues()).isEmpty();
    }

    @Test
    void testGetValuesIsUnmodifiable() {
        Context context = new Context();
        CapturedValue id = new CapturedValue("id", 42);
        context.putValue(id);
        Collection<CapturedValue> values = context.getValues();

        assertThatThrownBy(() -> values.add(new CapturedValue("x", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> values.remove(id)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(values::clear).isInstanceOf(UnsupportedOperationException.class);
        Iterator<CapturedValue> iterator = values.iterator();
        iterator.next();
        assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        assertThat(context.getValues()).containsExactly(id);
    }

    @Test
    void testGetValuesIsLiveView() {
        Context context = new Context();
        Collection<CapturedValue> values = context.getValues();
        CapturedValue id = new CapturedValue("id", 42);

        context.putValue(id);
        assertThat(values).containsExactly(id);
    }

    @Test
    void testPutAllEmptyCollection() {
        Context context = new Context();
        CapturedValue id = new CapturedValue("id", 42);
        context.putValue(id);

        context.putAll(List.of());
        assertThat(context.getValues()).containsExactly(id);
    }

    @Test
    void testPutAllReplacesExistingValue() {
        Context context = new Context();
        context.putValue(new CapturedValue("status", "in-process"));
        CapturedValue completed = new CapturedValue("status", "completed");
        CapturedValue id = new CapturedValue("id", 42);

        context.putAll(List.of(completed, id));
        assertThat(context.getValues()).containsExactly(id, completed);
    }

    @Test
    void testPutAllDuplicateNamesLastWins() {
        Context context = new Context();
        CapturedValue first = new CapturedValue("status", "first");
        CapturedValue last = new CapturedValue("status", "last");

        context.putAll(List.of(first, last));
        assertThat(context.getValues()).containsExactly(last);
    }

    @Test
    void testNullValueIsStored() {
        Context context = new Context();
        CapturedValue nullValue = new CapturedValue("status", CapturedValue.NULL_VALUE);

        context.putValue(nullValue);
        assertThat(context.find("status")).isEqualTo(nullValue);
        assertThat(context.getValues()).containsExactly(nullValue);
    }

    @Test
    void testMissingValueIsRejected() {
        Context context = new Context();
        CapturedValue id = new CapturedValue("id", 42);
        CapturedValue missing = new CapturedValue("status", CapturedValue.MISSING_VALUE);

        assertThatThrownBy(() -> context.putValue(missing)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> context.putAll(List.of(id, missing)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(context.getValues()).isEmpty();
    }

    @Test
    void testMultipleThreads() throws InterruptedException {
        Duration waitForFailure = Duration.ofSeconds(2);
        Context context = new Context();
        for(int i = 0; i < NUM_VALUES; ++i) {
            CapturedValue value = new CapturedValue(valueName(i), NUM_VALUES - i);
            context.putValue(value);
        }

        AtomicInteger errorCount = new AtomicInteger();
        Collection<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            Thread thread = new Thread(() -> {
                LocalDateTime time = LocalDateTime.now().plus(waitForFailure);
                while (LocalDateTime.now().isBefore(time) &&  errorCount.get() == 0) {
                    try {
                        CapturedValue value1 = context.find(randomValueName());
                        CapturedValue value2 = new CapturedValue(value1.name(), randomNewValue());
                        context.putValue(value2);
                    } catch(Throwable e) {
                        errorCount.incrementAndGet();
                        System.out.println(e.getMessage());
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        for(int i = 0; i < 20; i++) {
            Thread thread = new Thread(() -> {
                LocalDateTime time = LocalDateTime.now().plus(waitForFailure);
                while (LocalDateTime.now().isBefore(time) &&  errorCount.get() == 0) {
                    try {
                        List<CapturedValue> batch = IntStream.range(0, 10)
                                .mapToObj(j -> new CapturedValue(randomValueName(), randomNewValue()))
                                .toList();
                        context.putAll(batch);
                    } catch(Throwable e) {
                        errorCount.incrementAndGet();
                        System.out.println(e.getMessage());
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        for(int i = 0; i < 100; i++) {
            Thread thread = new Thread(() -> {
                LocalDateTime time = LocalDateTime.now().plus(waitForFailure);
                while (LocalDateTime.now().isBefore(time) && errorCount.get() == 0) {
                    try {
                        Collection<CapturedValue> values = context.getValues();
                        List.copyOf(values);
                    } catch(Throwable e) {
                        errorCount.incrementAndGet();
                        System.out.println(e.getMessage());
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(errorCount.get()).isEqualTo(0);
        List<String> expectedNames = IntStream.range(0, NUM_VALUES).mapToObj(this::valueName).toList();
        assertThat(context.getValues()).extracting(CapturedValue::name).containsExactlyElementsOf(expectedNames);
    }

    private String valueName(int i) {
        return String.format("id-%04d", i);
    }

    private String randomValueName() {
        return valueName(ThreadLocalRandom.current().nextInt(NUM_VALUES));
    }

    private int randomNewValue() {
        return -ThreadLocalRandom.current().nextInt(NUM_VALUES);
    }
}
