package com.instrumentalid.isc.connectors.readers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom Jackson deserializer that handles flexible boolean parsing.
 * Accepts: boolean values, strings like "true"/"false" (case-insensitive),
 * and null values (defaults to false).
 */
public class FlexibleBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        switch (p.currentToken()) {
            case VALUE_TRUE:
                return true;
            case VALUE_FALSE:
                return false;
            case VALUE_STRING:
                String stringValue = p.getText();
                if (stringValue == null || stringValue.trim().isEmpty()) {
                    return false;
                }
                return stringValue.trim().equalsIgnoreCase("true");
            case VALUE_NULL:
                return false;
            default:
                throw ctxt.weirdStringException(p.getText(), Boolean.class,
                    "Expected a boolean or string value");
        }
    }

    @Override
    public Boolean getNullValue(DeserializationContext ctxt) {
        return false;
    }
}

