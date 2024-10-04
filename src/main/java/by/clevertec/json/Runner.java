package by.clevertec.json;

import by.clevertec.json.dto.Customer;
import by.clevertec.json.parser.JsonToObjectParser;
import by.clevertec.json.parser.impl.JsonToObjectParserImpl;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;

import static by.clevertec.json.helper.StringConstants.DATETIME_OFFSET_CUSTOM_PATTERN;
import static by.clevertec.json.helper.StringConstants.TEST_JSON_STRING;

public class Runner {
    public static void main(String[] args) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_OFFSET_CUSTOM_PATTERN);
        JsonToObjectParser parser = new JsonToObjectParserImpl(formatter);
        try {
            Customer object = parser.toObject(TEST_JSON_STRING, Customer.class);
            System.out.println(object);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
