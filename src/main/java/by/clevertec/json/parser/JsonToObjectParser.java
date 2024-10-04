package by.clevertec.json.parser;

import java.lang.reflect.InvocationTargetException;

public interface JsonToObjectParser {

    <T> T toObject(String json, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException;
}
