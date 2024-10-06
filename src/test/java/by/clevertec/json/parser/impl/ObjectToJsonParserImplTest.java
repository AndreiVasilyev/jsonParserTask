package by.clevertec.json.parser.impl;

import by.clevertec.json.dto.Customer;
import by.clevertec.json.exception.JsonSyntaxException;
import by.clevertec.json.parser.ObjectToJsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;


import java.time.format.DateTimeFormatter;

import static by.clevertec.json.helper.StringData.DATETIME_OFFSET_CUSTOM_PATTERN;
import static by.clevertec.json.helper.StringData.TEST_JSON_STRING;
import static org.junit.jupiter.api.Assertions.*;

class ObjectToJsonParserImplTest {

    private final ObjectToJsonParser objectToJsonParser = new ObjectToJsonParserImpl();

    @Test
    void shouldParseObjectAsJacksonParser() throws JsonProcessingException, JsonSyntaxException {
        //given
        ObjectMapper objectMapper = new ObjectMapper();
        Customer customer = new JsonToObjectParserImpl(DateTimeFormatter.ofPattern(DATETIME_OFFSET_CUSTOM_PATTERN))
                .toObject(TEST_JSON_STRING, Customer.class);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        String expectedJson = objectMapper.writeValueAsString(customer);

        //when
        String formattedJson = objectToJsonParser.toJson(customer);
        String actualJson = formattedJson.trim()
                .replace(" ", "")
                .replace("\n", "")
                .replace("\t", "");
        //then
        assertEquals(expectedJson, actualJson);
    }

}