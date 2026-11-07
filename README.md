# Alchesay

Alchesay is a workflow tracking system. It is in early development.

## Modules

| Module             | Description                                                                 |
|--------------------|-----------------------------------------------------------------------------|
| `alchesay-tracker` | Spring Boot application that captures values from JSON events. |

### Capturing values from JSON

`ValueReference` names a location in a JSON document using a
[JSON Pointer](https://datatracker.ietf.org/doc/html/rfc6901) path. Calling `capture` on a
document returns a `CapturedValue` holding the value converted to plain Java types.

```java
JsonNode event = new ObjectMapper().readTree("""
        { "order": { "id": 42, "tags": ["new", "priority"] } }
        """);

new ValueReference("orderId", "/order/id").capture(event).value();    // 42 (Integer)
new ValueReference("tags", "/order/tags").capture(event).value();     // ["new", "priority"] (unmodifiable List)
new ValueReference("missing", "/order/total").capture(event).value(); // CapturedValue.MISSING_VALUE
```

| JSON                | Java                                                   |
|---------------------|--------------------------------------------------------|
| string              | `String`                                               |
| number              | matching `Number` subtype (`Integer`, `Long`, `Double`, ...) |
| boolean             | `Boolean`                                              |
| array               | unmodifiable `List`                                    |
| object              | unmodifiable `Map<String, Object>`, field order preserved |
| `null`              | `CapturedValue.NULL_VALUE`                             |
| path not present    | `CapturedValue.MISSING_VALUE`                          |

## Building

Requires Java 25. The Gradle wrapper downloads the correct Gradle version.

```shell
./gradlew build    # compile and run tests
./gradlew test     # run tests only
./gradlew :alchesay-tracker:bootRun
```

## Contributing

Issues and pull requests are welcome. Please include unit tests with any change.

## License

Copyright 2026 The Nine Seed, LLC

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE) for attribution.
