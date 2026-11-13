package com.nineseed.alchesay.tracker.workflow;

import com.nineseed.alchesay.tracker.event.CapturedValue;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * The captured values accumulated for a workflow, keyed by name.
 * <p>
 * Values are ordered by name, ignoring case; names that differ only in case are distinct entries, ordered
 * by {@link String#compareTo}. Lookups match names exactly. Putting a value whose name is already present
 * replaces the earlier value. Values holding {@link CapturedValue#MISSING_VALUE} are rejected.
 * <p>
 * This class is thread-safe.
 */
public class Context {
    private static final Comparator<String> NAME_ORDER =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private final Map<String, CapturedValue> values = new ConcurrentSkipListMap<>(NAME_ORDER);

    /**
     * Adds a captured value, replacing any existing value with the same name.
     *
     * @param value the value to add
     * @throws IllegalArgumentException if {@code value} holds {@link CapturedValue#MISSING_VALUE}
     */
    public void putValue(@NonNull CapturedValue value) {
        checkNotMissing(value);
        values.put(value.name(), value);
    }

    /**
     * @return an unmodifiable view of the values, ordered by name. The view reflects later changes, and
     *         iterating it while other threads modify the context never throws
     *         {@link ConcurrentModificationException}.
     */
    public Collection<CapturedValue> getValues() {
        return Collections.unmodifiableCollection(values.values());
    }

    /**
     * Finds the value with the given name.
     *
     * @param name the name to look up
     * @return the value, or a value holding {@link CapturedValue#MISSING_VALUE} if none has that name
     */
    public CapturedValue find(@NonNull String name) {
        return values.getOrDefault(name, new CapturedValue(name, CapturedValue.MISSING_VALUE));
    }

    /**
     * Adds each captured value, replacing any existing value with the same name. When the collection holds
     * several values with the same name, the last one wins.
     * <p>
     * Every value is checked before any is added, so an invalid value leaves the context unchanged. The values
     * are not added atomically, though: other threads may see some of them before the rest are added.
     *
     * @param valueCollection the values to add
     * @throws NullPointerException     if {@code valueCollection} or any of its values is {@code null}
     * @throws IllegalArgumentException if any value holds {@link CapturedValue#MISSING_VALUE}
     */
    public void putAll(@NonNull Collection<CapturedValue> valueCollection) {
        List<CapturedValue> checked = List.copyOf(valueCollection);
        checked.forEach(Context::checkNotMissing);
        checked.forEach(value -> values.put(value.name(), value));
    }

    private static void checkNotMissing(CapturedValue value) {
        if (value.value() == CapturedValue.MISSING_VALUE) {
            throw new IllegalArgumentException("Cannot store '" + value.name() + "' because its value is MISSING_VALUE");
        }
    }
}
