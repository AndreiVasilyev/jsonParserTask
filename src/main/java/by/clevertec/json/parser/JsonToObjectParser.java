package by.clevertec.json.parser;

import by.clevertec.json.exception.JsonSyntaxException;


public interface JsonToObjectParser {

    <T> T toObject(String json, Class<T> clazz) throws JsonSyntaxException;
}