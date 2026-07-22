package com.instrumentalid.isc.connectors.readers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom Jackson deserializer that handles flexible integer parsing.
 * Accepts: integer values, strings representing integers, and null values (defaults to specified default).
 */
public class FlexibleIntegerDeserializer extends JsonDeserializer<Integer> {
    private final int defaultValue;

    public FlexibleIntegerDeserializer(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    public FlexibleIntegerDeserializer() {
        this(0);
    }

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        switch (p.currentToken()) {
            case VALUE_NUMBER_INT:
                return p.getIntValue();
            case VALUE_STRING:
                String stringValue = p.getText();
                if (stringValue == null || stringValue.trim().isEmpty()) {
                    return defaultValue;
                }
                try {
                    return Integer.parseInt(stringValue.trim());
                } catch (NumberFormatException e) {
                    throw ctxt.weirdStringException(stringValue, Integer.class,
                        "Expected a valid integer");
                }
            case VALUE_NULL:
                return defaultValue;
            default:
                throw ctxt.weirdStringException(p.getText(), Integer.class,
                    "Expected an integer or string value");
        }
    }

    @Override
    public Integer getNullValue(DeserializationContext ctxt) {
        return defaultValue;
    }
}

