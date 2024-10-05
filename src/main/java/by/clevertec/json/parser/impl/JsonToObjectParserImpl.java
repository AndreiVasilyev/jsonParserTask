package by.clevertec.json.parser.impl;


import by.clevertec.json.dto.help.ParserResult;
import by.clevertec.json.exception.JsonSyntaxException;
import by.clevertec.json.parser.JsonToObjectParser;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static by.clevertec.json.helper.StringConstants.*;


public class JsonToObjectParserImpl implements JsonToObjectParser {

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;


    public JsonToObjectParserImpl() {
    }

    public JsonToObjectParserImpl(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public <T> T toObject(String json, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String preparedJson = json.replace(NEWLINE, EMPTY).replace(SPACE, EMPTY).trim();
        return parseObject(preparedJson, clazz);
    }

    private <T> T parseObject(String json, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (clazz.equals(Map.class)) {
            if (clazz.getTypeParameters().length == 0) {
                throw new JsonSyntaxException("Map must have type parameters");
            }
            return (T) parseMap(json, clazz);
        }
        T currentObject = clazz.getConstructor().newInstance();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Matcher matcher = prepareMatcher(json, FIELD_START_TEMPLATE, declaredField.getName());
            if (matcher.find()) {
                String matchGroup = matcher.group();
                char startSymbol=matchGroup.charAt(matchGroup.length() - 1);
                Object fieldInstance = switch (startSymbol) {
                    case OPEN_QUOTE: {
                        Matcher stringMatcher = prepareMatcher(json, FIELD_STRING_TEMPLATE, declaredField.getName());
                        if (!stringMatcher.find())
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing string field");
                        String stringField = stringMatcher.group();
                        json = json.replace(stringField, EMPTY);
                        String fieldValue = stringField.replace(declaredField.getName(), EMPTY)
                                .replace(QUOTE, EMPTY)
                                .replace(COMMA, EMPTY)
                                .replaceFirst(COLON, EMPTY);
                        Class<?> fieldClazz = declaredField.getType();
                        yield parseStringValue(fieldValue, fieldClazz);
                    }
                    case OPEN_CURLY_BRACE: {
                        Matcher objectMatcher = prepareMatcher(json, FIELD_OBJECT_TEMPLATE, declaredField.getName());
                        if (!objectMatcher.find())
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing object field");
                        String objectField = objectMatcher.group();
                        String fieldValue = objectField.replace(declaredField.getName(), EMPTY)
                                .replaceFirst(COLON, EMPTY)
                                .replaceFirst(QUOTE + QUOTE, EMPTY);
                        fieldValue = extractObjectValue(fieldValue);
                        json = json.replace((QUOTE + declaredField.getName() + QUOTE + COLON), EMPTY)
                                .replace(fieldValue, EMPTY);
                        if (declaredField.getType().isAssignableFrom(Map.class)) {
                            yield parseMap(fieldValue, declaredField.getGenericType());
                        } else {
                            yield parseObject(fieldValue, declaredField.getClass());
                        }
                    }
                    case OPEN_SQUARE_BRACKET: {
                        Matcher collectionMatcher = prepareMatcher(json, FIELD_ARRAY_TEMPLATE, declaredField.getName());
                        if (!collectionMatcher.find())
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing string field");
                        String collectionField = collectionMatcher.group();
                        json = json.replace(collectionField, EMPTY);
                        String fieldValue = collectionField.replace(declaredField.getName(), EMPTY);
                        fieldValue = fieldValue.substring(4, fieldValue.length() - 1);
                        Class<?> genericType = (Class<?>) ((ParameterizedType) declaredField.getGenericType())
                                .getActualTypeArguments()[0];
                        yield parseCollection(fieldValue, genericType);
                    }
                    default:
                        if (!Character.isDigit(startSymbol)) {
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing number field");
                        }
                        Matcher digitsMatcher = prepareMatcher(json, FIELD_DIGITS_TEMPLATE, declaredField.getName());
                        if (!digitsMatcher.find())
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing number field");
                        String numberField = digitsMatcher.group();
                        json = json.replace(numberField, EMPTY);
                        String fieldValue = numberField.replace(declaredField.getName(), EMPTY)
                                .replace(QUOTE, EMPTY)
                                .replace(COMMA, EMPTY)
                                .replaceFirst(COLON, EMPTY);
                        Class<?> fieldClazz = declaredField.getType();
                        yield parseNumberValue(fieldValue, fieldClazz);
                };
                declaredField.setAccessible(true);
                declaredField.set(currentObject, fieldInstance);
            }
        }
        return currentObject;
    }


    private Matcher prepareMatcher(String sourceString, String template, String templateValue) {
        String fieldStartRegex = String.format(template, templateValue);
        Pattern pattern = Pattern.compile(fieldStartRegex);
        return pattern.matcher(sourceString);
    }

    private Collection<Object> parseCollection(String json, Class<?> collectionType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException {
        Collection<Object> collection = new ArrayList<>();
        if (collectionType.isAssignableFrom(Set.class)) {
            collection = new HashSet<>();
        }
        if (json.startsWith(String.valueOf(OPEN_CURLY_BRACE))) {
            while (!json.isEmpty()) {
                String collectionElement = extractObjectValue(json);
                Object parsedObject = parseObject(collectionElement, collectionType);
                collection.add(parsedObject);
                json = json.replace(collectionElement, EMPTY);
                if (json.startsWith(COMMA)&&json.length()==1) break;
            }
        }
        return collection;
    }

    private Object parseStringValue(String value, Class<?> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        if (clazz.equals(String.class)) {
            return value;
        }
        if (clazz.equals(OffsetDateTime.class)) {
            Method method = clazz.getMethod(PARSE_METHOD_NAME, CharSequence.class, DateTimeFormatter.class);
            return method.invoke(null, value, dateTimeFormatter);
        }
        try {
            return clazz.getDeclaredConstructor(String.class).newInstance(value);
        } catch (NoSuchMethodException e) {
            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method method : declaredMethods) {
                Class<?>[] typeParameters = method.getParameterTypes();
                Class<?> returnType = method.getReturnType();
                if (typeParameters.length == 1
                        && typeParameters[0].isAssignableFrom(String.class)
                        && returnType.isAssignableFrom(clazz)
                        && Modifier.isPublic(method.getModifiers())) {
                    return method.invoke(null, value);
                }
            }
            throw new JsonSyntaxException("Invalid JSON syntax when parsing object field");
        }
    }

    private Object parseNumberValue(String value, Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (clazz.equals(BigDecimal.class) || clazz.equals(BigInteger.class)) {
            Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
            return constructor.newInstance(value);
        }
        Method method;
        if (clazz.isPrimitive()) {
            String methodName = PARSE_METHOD_NAME + clazz.getSimpleName();
            method = clazz.getMethod(methodName, String.class);
        } else {
            method = clazz.getMethod(VALUE_OF_METHOD_NAME, String.class);
        }
        return method.invoke(null, value);
    }


    private Object parseMap(String json, Type type) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException {
        Map<Object, Object> map = new HashMap<>();
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class<?> keyType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        Class<?> valueType = (Class<?>) parameterizedType.getActualTypeArguments()[1];
        json = json.substring(1, json.length() - 1);
        while (!json.isEmpty()) {
            ParserResult parserResult = parseObjectForMap(json, keyType);
            Object keyInstance = parserResult.instance();
            json = parserResult.json();
            json = json.replaceFirst(COLON, EMPTY);
            parserResult = parseObjectForMap(json, valueType);
            Object valueInstance = parserResult.instance();
            json = parserResult.json();
            map.put(keyInstance, valueInstance);
        }
        return map;
    }

    private ParserResult parseObjectForMap(String json, Class<?> type) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException {
        char startSymbol = json.charAt(0);
        return switch (startSymbol) {
            case '"': {
                String stringValue = extractStringValue(json);
                json = json.replaceFirst(stringValue, EMPTY);
                stringValue = stringValue.substring(1, stringValue.length() - 1);
                Object instance = parseStringValue(stringValue, type);
                yield new ParserResult(json, instance);
            }
            case '{': {
                //TODO handle case with object
                yield null;
            }
            case '[': {
                //TODO handle case with collection
                yield null;
            }
            default: {
                if (!Character.isDigit(startSymbol)) {
                    throw new JsonSyntaxException("Invalid JSON syntax when parsing number field");
                }
                String numberValue = extractNumberValue(json);
                json = json.replaceFirst(numberValue, EMPTY).replaceFirst(COMMA, EMPTY);
                Object instance = parseNumberValue(numberValue, type);
                yield new ParserResult(json, instance);
            }
        };
    }

    private String extractStringValue(String json) {
        Deque<Character> delimitersStack = new LinkedList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : json.toCharArray()) {
            stringBuilder.append(character);
            if (character.equals(OPEN_QUOTE)) {
                delimitersStack.push(character);
            }
            if (delimitersStack.size() == 2) {
                break;
            }
        }
        return stringBuilder.toString();
    }

    private String extractNumberValue(String json) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : json.toCharArray()) {
            if (character.equals(COMMA_CHAR)) {
                break;
            }
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }

    private String extractObjectValue(String fieldValue) {
        Deque<Character> openBrackets = new LinkedList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : fieldValue.toCharArray()) {
            if (stringBuilder.isEmpty() && character.equals(COMMA_CHAR)) {
                fieldValue = fieldValue.replaceFirst(COMMA, EMPTY);
                continue;
            }
            stringBuilder.append(character);
            if (character.equals(OPEN_CURLY_BRACE) || character.equals(OPEN_SQUARE_BRACKET)) {
                openBrackets.push(character);
            } else if (character.equals(CLOSE_CURLY_BRACE) || character.equals(CLOSE_SQUARE_BRACKET)) {
                openBrackets.pop();
            }
            if (openBrackets.isEmpty()) {
                break;
            }
        }
        return stringBuilder.toString();
    }

}

