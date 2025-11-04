package io.github.chubbyhippo.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("test serialize object to JSON")
    void testSerializeToJson() throws JsonProcessingException {
        Person person = new Person("John", 30);

        String json = objectMapper.writeValueAsString(person);

        assertThat(json).contains("John");
        assertThat(json).contains("30");
    }

    @Test
    @DisplayName("test deserialize JSON to object")
    void testDeserializeFromJson() throws JsonProcessingException {
        String json = "{\"name\":\"Jane\",\"age\":25}";

        Person person = objectMapper.readValue(json, Person.class);

        assertThat(person.name()).isEqualTo("Jane");
        assertThat(person.age()).isEqualTo(25);
    }

    @Test
    @DisplayName("test serialize and deserialize roundtrip")
    void testRoundTrip() throws JsonProcessingException {
        Person original = new Person("Bob", 40);

        String json = objectMapper.writeValueAsString(original);
        Person deserialized = objectMapper.readValue(json, Person.class);

        assertThat(deserialized.name()).isEqualTo(original.name());
        assertThat(deserialized.age()).isEqualTo(original.age());
    }

    // Simple record for testing
    record Person(String name, int age) {
    }
}