package by.clevertec.json.parser.impl;

import by.clevertec.json.exception.ObjectToJsonParsingException;
import by.clevertec.json.parser.ObjectToJsonParser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static by.clevertec.json.helper.StringConstants.CLOSE_CURLY_BRACE;
import static by.clevertec.json.helper.StringConstants.CLOSE_SQUARE_BRACKET;
import static by.clevertec.json.helper.StringConstants.COLON;
import static by.clevertec.json.helper.StringConstants.COMMA;
import static by.clevertec.json.helper.StringConstants.NEWLINE;
import static by.clevertec.json.helper.StringConstants.OPEN_CURLY_BRACE;
import static by.clevertec.json.helper.StringConstants.OPEN_SQUARE_BRACKET;
import static by.clevertec.json.helper.StringConstants.QUOTE;
import static by.clevertec.json.helper.StringConstants.SPACE;
import static by.clevertec.json.helper.StringConstants.TAB;

public class ObjectToJsonParserImpl implements ObjectToJsonParser {

    private StringBuilder tabSting;

    public ObjectToJsonParserImpl() {
        this.tabSting = new StringBuilder();
    }

    @Override
    public String toJson(Object obj) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            parseObject(obj, stringBuilder);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new ObjectToJsonParsingException("ObjectToJsonParsingException");
        }
        return stringBuilder.toString();
    }

    private void parseObject(Object object, StringBuilder stringBuilder) throws IllegalAccessException, NoSuchMethodException {
        stringBuilder.append(tabSting).append(OPEN_CURLY_BRACE);
        tabSting.append(TAB);
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isTransient(field.getModifiers())) {
                Class<?> fieldType = field.getType();
                field.setAccessible(true);
                stringBuilder.append(NEWLINE)
                        .append(tabSting)
                        .append(QUOTE)
                        .append(field.getName())
                        .append(QUOTE)
                        .append(COLON)
                        .append(SPACE);
                if (fieldType.isPrimitive()) {
                    String fieldValue = getPrimitiveFieldValue(field, object);
                    stringBuilder.append(fieldValue).append(COMMA);
                } else if (isPrimitiveWrapper(fieldType)) {
                    Object fieldInstance = field.get(object);
                    String fieldValue = fieldInstance.toString();
                    stringBuilder.append(fieldValue).append(COMMA);
                } else if (fieldType.equals(String.class)) {
                    String fieldValue = (String) field.get(object);
                    stringBuilder.append(QUOTE).append(fieldValue).append(QUOTE).append(COMMA);
                } else if (fieldType.isAssignableFrom(List.class) || fieldType.isAssignableFrom(Set.class)) {
                    Collection<?> collection = (Collection<?>) field.get(object);
                    parseCollection(collection, stringBuilder);
                    stringBuilder.append(COMMA);
                } else if (fieldType.isAssignableFrom(Map.class)) {
                    Map<?, ?> map = (Map<?, ?>) field.get(object);
                    parseMap(map, stringBuilder);
                } else if (fieldType.isAssignableFrom(UUID.class)
                        || fieldType.getName().contains("date")
                        || fieldType.getName().contains("time")) {
                    Object fieldInstance = field.get(object);
                    String fieldValue = fieldInstance.toString();
                    stringBuilder.append(QUOTE).append(fieldValue).append(QUOTE).append(COMMA);
                } else {
                    Object fieldObj = field.get(object);
                    parseObject(fieldObj, stringBuilder);
                }

            }
        }
        tabSting.delete(0, 1);
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder
                .append(NEWLINE)
                .append(tabSting)
                .append(CLOSE_CURLY_BRACE);
    }

    private void parseMap(Map<?, ?> map, StringBuilder stringBuilder) throws IllegalAccessException, NoSuchMethodException {
        tabSting.append(TAB);
        stringBuilder.append(OPEN_CURLY_BRACE);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            stringBuilder.append(NEWLINE)
                    .append(tabSting);
            parseMapElement(key, stringBuilder);
            stringBuilder.append(COLON).append(SPACE);
            parseMapElement(value, stringBuilder);
            stringBuilder.append(COMMA);
        }
        tabSting.delete(0, 1);
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(NEWLINE)
                .append(tabSting)
                .append(CLOSE_CURLY_BRACE)
                .append(COMMA);
    }

    private void parseMapElement(Object object, StringBuilder stringBuilder) throws IllegalAccessException, NoSuchMethodException {
        if (object.getClass().isAssignableFrom(String.class)
                || object.getClass().isAssignableFrom(UUID.class)) {
            String objectValue = object.toString();
            stringBuilder.append(QUOTE)
                    .append(objectValue)
                    .append(QUOTE);
        } else if (isPrimitiveWrapper(object.getClass())
                || object.getClass().isAssignableFrom(BigDecimal.class)) {
            String objectValue = object.toString();
            stringBuilder.append(objectValue);
        } else {
            parseObject(object, stringBuilder);
        }
    }

    private void parseCollection(Collection<?> collection, StringBuilder stringBuilder) throws IllegalAccessException, NoSuchMethodException {
        tabSting.append(TAB);
        stringBuilder.append(OPEN_SQUARE_BRACKET).append(NEWLINE);
        for (Object object : collection) {
            parseObject(object, stringBuilder);
            stringBuilder.append(COMMA);
        }
        tabSting.delete(0, 1);
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(NEWLINE)
                .append(tabSting)
                .append(CLOSE_SQUARE_BRACKET);
    }

    private boolean isPrimitiveWrapper(Class<?> clazz) {
        return clazz.isAssignableFrom(Boolean.class)
                || clazz.isAssignableFrom(Byte.class)
                || clazz.isAssignableFrom(Character.class)
                || clazz.isAssignableFrom(Short.class)
                || clazz.isAssignableFrom(Integer.class)
                || clazz.isAssignableFrom(Long.class)
                || clazz.isAssignableFrom(Float.class)
                || clazz.isAssignableFrom(Double.class);
    }

    private String getPrimitiveFieldValue(Field field, Object object) throws IllegalAccessException {
        return switch (field.getName()) {
            case "boolean": {
                yield String.valueOf(field.getBoolean(object));
            }
            case "byte": {
                yield String.valueOf(field.getByte(object));
            }
            case "char": {
                yield String.valueOf(field.getChar(object));
            }
            case "short": {
                yield String.valueOf(field.getShort(object));
            }
            case "int": {
                yield String.valueOf(field.getInt(object));
            }
            case "long": {
                yield String.valueOf(field.getLong(object));
            }
            case "float": {
                yield String.valueOf(field.getFloat(object));
            }
            case "double": {
                yield String.valueOf(field.getDouble(object));
            }
            default:
                yield "";
        };
    }
}
