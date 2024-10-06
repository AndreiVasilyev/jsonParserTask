package by.clevertec.json.parser.impl;

import by.clevertec.json.dto.Customer;
import by.clevertec.json.exception.JsonSyntaxException;
import by.clevertec.json.helper.OffsetDateTimeDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static by.clevertec.json.helper.StringData.DATETIME_OFFSET_CUSTOM_PATTERN;
import static by.clevertec.json.helper.StringData.TEST_JSON_BAD_STRING;
import static by.clevertec.json.helper.StringData.TEST_JSON_STRING;
import static org.junit.jupiter.api.Assertions.*;

class JsonToObjectParserImplTest {

    private final JsonToObjectParserImpl jsonToObjectParser = new JsonToObjectParserImpl(DateTimeFormatter.ofPattern(DATETIME_OFFSET_CUSTOM_PATTERN));


    @Test
    void shouldParseJsonAsJacksonParser() throws JsonProcessingException {
        //given
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new SimpleModule().addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer()));
        Customer expectedCustomer = objectMapper.readValue(TEST_JSON_STRING, Customer.class);

        //when
        Customer actualCustomer = jsonToObjectParser.toObject(TEST_JSON_STRING, Customer.class);

        //then
        assertEquals(expectedCustomer, actualCustomer);
    }

    @Test
    void shouldThrowExceptionWhenJsonWrongSyntax() {
        //given when then
        assertThrows(JsonSyntaxException.class, () -> jsonToObjectParser.toObject(TEST_JSON_BAD_STRING, Customer.class));
    }
}