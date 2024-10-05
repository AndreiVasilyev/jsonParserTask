package by.clevertec.json;

import by.clevertec.json.dto.Customer;
import by.clevertec.json.parser.JsonToObjectParser;
import by.clevertec.json.parser.ObjectToJsonParser;
import by.clevertec.json.parser.impl.JsonToObjectParserImpl;
import by.clevertec.json.parser.impl.ObjectToJsonParserImpl;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;

import static by.clevertec.json.helper.StringConstants.DATETIME_OFFSET_CUSTOM_PATTERN;
import static by.clevertec.json.helper.StringConstants.TEST_JSON_STRING;

public class Runner {
    public static void main(String[] args) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_OFFSET_CUSTOM_PATTERN);
        JsonToObjectParser objectParser = new JsonToObjectParserImpl(formatter);
        ObjectToJsonParser jsonParser = new ObjectToJsonParserImpl();
        try {
            Customer object = objectParser.toObject(TEST_JSON_STRING, Customer.class);
            System.out.println("\n\n"+object+"\n\n");
            System.out.println(jsonParser.toJson(object));
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
