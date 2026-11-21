package com.multios.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Shared Jackson configuration for Path-aware JSON persistence of Java records.
 */
final class JacksonConfig {

    private JacksonConfig() {
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule pathModule = new SimpleModule("PathModule");
        pathModule.addSerializer(Path.class, new JsonSerializer<>() {
            @Override
            public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.toString());
            }
        });
        pathModule.addDeserializer(Path.class, new JsonDeserializer<>() {
            @Override
            public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Path.of(p.getValueAsString());
            }
        });
        mapper.registerModule(pathModule);
        return mapper;
    }
}
