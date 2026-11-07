package com.nineseed.alchesay.tracker.event;

import lombok.NonNull;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nineseed.alchesay.tracker.event.CapturedValue.MISSING_VALUE;
import static com.nineseed.alchesay.tracker.event.CapturedValue.NULL_VALUE;

/**
 * A named reference to a location in a JSON document, expressed as a
 * <a href="https://datatracker.ietf.org/doc/html/rfc6901">JSON Pointer</a> path.
 * <p>
 * A {@code ValueReference} is immutable and reusable: each call to {@link #capture(JsonNode)} resolves
 * the path against a document and returns a new {@link CapturedValue}. The JSON is converted to plain
 * Java types:
 * <ul>
 *     <li>string &rarr; {@link String}</li>
 *     <li>number &rarr; the matching {@link Number} subtype (e.g. {@link Integer}, {@link Double})</li>
 *     <li>boolean &rarr; {@link Boolean}</li>
 *     <li>array &rarr; unmodifiable {@link List}, with elements converted recursively</li>
 *     <li>object &rarr; unmodifiable {@link Map}{@code <String, Object>} preserving field order, with values converted recursively</li>
 *     <li>JSON {@code null} &rarr; {@link CapturedValue#NULL_VALUE}</li>
 *     <li>path not present &rarr; {@link CapturedValue#MISSING_VALUE}</li>
 * </ul>
 * Equality is based on {@code name} and {@code jsonPath}.
 *
 * @param name     The name given to values captured through this reference.
 * @param jsonPath The JSON Pointer path (e.g. {@code /baz/qux}) used to locate the value.
 */
public record ValueReference(String name, String jsonPath) {
    /**
     * Creates a value reference.
     *
     * @param name     the name given to values captured through this reference
     * @param jsonPath the JSON Pointer path used to locate the value, e.g. {@code /foo}
     * @throws NullPointerException if {@code name} or {@code jsonPath} is {@code null}
     * @throws IllegalArgumentException if {@code jsonPath} is not a valid JSON Pointer
     */
    public ValueReference(@NonNull String name, @NonNull String jsonPath) {
        JsonPointer.compile(jsonPath);
        this.name = name;
        this.jsonPath = jsonPath;
    }

    /**
     * Resolves {@link #jsonPath()} against the given document and returns the result.
     *
     * @param rootNode the root of the JSON document to capture from
     * @return a new {@link CapturedValue} with this reference's name and the converted value
     * @throws NullPointerException if {@code rootNode} is {@code null}
     */
    public CapturedValue capture(@NonNull JsonNode rootNode) {
        return new CapturedValue(name, toJavaValue(rootNode.at(jsonPath)));
    }

    /**
     * Converts a JSON node to its plain Java equivalent, recursing into arrays and objects.
     *
     * @param node the node to convert
     * @return the converted value; see the class documentation for the type mapping
     */
    private static Object toJavaValue(JsonNode node) {
        Object value;
        if (node.isMissingNode()) {
            value = MISSING_VALUE;
        } else if (node.isNull()) {
            value = NULL_VALUE;
        } else if (node.isString()) {
            value = node.stringValue();
        } else if (node.isNumber()) {
            value = node.numberValue();
        } else if (node.isBoolean()) {
            value = node.booleanValue();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>(node.size());
            for (JsonNode element : node) {
                list.add(toJavaValue(element));
            }
            value = Collections.unmodifiableList(list);
        } else if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                map.put(entry.getKey(), toJavaValue(entry.getValue()));
            }
            value = Collections.unmodifiableMap(map);
        } else {
            value = node;
        }

        return value;
    }
}
