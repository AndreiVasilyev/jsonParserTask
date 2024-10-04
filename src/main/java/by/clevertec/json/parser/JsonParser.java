package by.clevertec.json.parser;


import by.clevertec.json.exception.JsonSyntaxException;


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
import java.util.Arrays;
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


public class JsonParser {

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;


    public JsonParser() {
    }

    public JsonParser(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public <T> T toObject(String json, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String preparedJson = json.replace(NEWLINE, EMPTY).replace(SPACE, EMPTY).trim();
        return parseObject(preparedJson, clazz);
    }

    private <T> T parseObject(String json, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (clazz.equals(Map.class)) {
            System.out.println("Map=" + clazz.getTypeParameters()[0].getName());
            return (T) parseMap(json, clazz.getGenericSuperclass());
        }
        T currentObject = clazz.getConstructor().newInstance();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Matcher matcher = prepareMatcher(json, FIELD_START_TEMPLATE, declaredField.getName());
            if (matcher.find()) {
                String matchGroup = matcher.group();
                String symbol = matchGroup.substring(matchGroup.length() - 1);
                switch (symbol) {
                    case "\"": {
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
                        Object fieldInstance = parseStringValue(fieldValue, fieldClazz);
                        setFieldValue(declaredField, currentObject, fieldInstance);
                        break;
                    }
                    case "{": {
                        Matcher objectMatcher = prepareMatcher(json, FIELD_OBJECT_TEMPLATE, declaredField.getName());
                        if (!objectMatcher.find())
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing object field");
                        String objectField = objectMatcher.group();
                        String fieldValue = objectField.replace(declaredField.getName(), EMPTY)
                                .replaceFirst(COLON, EMPTY)
                                .replaceFirst(QUOTE + QUOTE, EMPTY);
                        Deque<Character> openBrackets = new LinkedList<>();
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Character character : fieldValue.toCharArray()) {
                            if (stringBuilder.isEmpty() && character.equals(',')) {
                                fieldValue = fieldValue.replaceFirst(COMMA, EMPTY);
                                continue;
                            }
                            stringBuilder.append(character);
                            if (character.equals('{') || character.equals('[')) {
                                openBrackets.push(character);
                            } else if (character.equals('}') || character.equals(']')) {
                                openBrackets.pop();
                            }
                            if (openBrackets.isEmpty()) {
                                break;
                            }
                        }
                        fieldValue = stringBuilder.toString();
                        json = json.replace((QUOTE + declaredField.getName() + QUOTE + COLON), EMPTY)
                                .replace(fieldValue, EMPTY);

                        Object parsedObject;
                        if (declaredField.getType().isAssignableFrom(Map.class)) {
                            parsedObject = parseMap(fieldValue, declaredField.getGenericType());
                        } else {
                            parsedObject = parseObject(fieldValue, declaredField.getClass());
                        }

                        setFieldValue(declaredField, currentObject, parsedObject);
                        break;
                    }
                    case "[": {
                        Matcher stringMatcher = prepareMatcher(json, FIELD_ARRAY_TEMPLATE, declaredField.getName());
                        if (!stringMatcher.find())
                            throw new JsonSyntaxException("Invalid JSON syntax when parsing string field");
                        String stringField = stringMatcher.group();
                        json = json.replace(stringField, EMPTY);
                        String fieldValue = stringField.replace(declaredField.getName(), EMPTY);
                        fieldValue = fieldValue.substring(4, fieldValue.length() - 1);
                        Class<?> collectionType = declaredField.getType();
                        String genericType = ((ParameterizedType) declaredField.getGenericType())
                                .getActualTypeArguments()[0]
                                .getTypeName();

                        Collection<Object> fieldInstance = new ArrayList<>();
                        if (collectionType.isAssignableFrom(Set.class)) {
                            fieldInstance = new HashSet<>();
                        }
                        parseCollection(fieldValue, fieldInstance, Class.forName(genericType));
                        setFieldValue(declaredField, currentObject, fieldInstance);
                        break;
                    }
                    default:
                        if (!Character.isDigit(symbol.charAt(0))) {
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
                        Object fieldInstance = parseNumberValue(fieldValue, fieldClazz);
                        setFieldValue(declaredField, currentObject, fieldInstance);


                }
            }
        }
        return currentObject;
    }

    private void setFieldValue(Field field, Object instanceWithField, Object fieldValue) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(instanceWithField, fieldValue);
        field.setAccessible(false);
    }

    private Matcher prepareMatcher(String sourceString, String template, String templateValue) {
        String fieldStartRegex = String.format(template, templateValue);
        Pattern pattern = Pattern.compile(fieldStartRegex);
        return pattern.matcher(sourceString);
    }

    private void parseCollection(String json, Collection<Object> collection, Class<?> collectionType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException {
        if (json.startsWith("{")) {
            Deque<Character> openBrackets = new LinkedList<>();
            StringBuilder stringBuilder = new StringBuilder();
            for (Character character : json.toCharArray()) {
                if (stringBuilder.isEmpty() && character.equals(',')) {
                    json = json.replaceFirst(COMMA, EMPTY);
                    continue;
                }
                stringBuilder.append(character);
                if (character.equals('{') || character.equals('[')) {
                    openBrackets.push(character);
                } else if (character.equals('}') || character.equals(']')) {
                    openBrackets.pop();
                }
                if (openBrackets.isEmpty()) {
                    Object object = parseObject(stringBuilder.toString(), collectionType);
                    collection.add(object);
                    json = json.replace(stringBuilder.toString(), EMPTY);
                    stringBuilder.setLength(0);
                }
            }
        }
    }

    private Object parseStringValue(String value, Class clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        System.out.println("start data=" + value + "\n start class=" + clazz);
        if (clazz.equals(String.class)) {
            return value;
        }
        if (clazz.equals(OffsetDateTime.class)) {
            Method method = clazz.getMethod("parse", CharSequence.class, DateTimeFormatter.class);
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
            String methodName = "parse" + clazz.getSimpleName();
            method = clazz.getMethod(methodName, String.class);
        } else {
            method = clazz.getMethod("valueOf", String.class);
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
            System.out.println("json="+json);
            Object keyInstance= parseObjectForMap(json, keyType);
            System.out.println("json after=" + json);
           /* char startSymbol = json.charAt(0);
            Object keyInstance = switch (startSymbol) {
                case '"': {
                    String keyValue = extractStringValue(json);
                    json = json.replaceFirst(keyValue, EMPTY);
                    keyValue = keyValue.substring(1, keyValue.length() - 1);
                    yield parseStringValue(keyValue, keyType);
                }
                case '{': {
                    System.out.println("object found");
                    yield null;
                }
                case '[': {
                    System.out.println("collection found");
                    yield null;
                }
                default: {
                    System.out.println("primitive found");
                    yield null;
                }
            };*/
            json = json.replaceFirst(COLON, EMPTY);
            Object valueInstance= parseObjectForMap(json, valueType);

            /*startSymbol = json.charAt(0);
            Object valueInstance = switch (startSymbol) {
                case '"': {
                    System.out.println("string found");
                    yield null;
                }
                case '{': {
                    System.out.println("object found");
                    yield null;
                }
                case '[': {
                    System.out.println("collection found");
                    yield null;
                }
                default: {
                    if (!Character.isDigit(startSymbol)) {
                        throw new JsonSyntaxException("Invalid JSON syntax when parsing number field");
                    }
                    String numberValue = extractNumberValue(json);
                    json = json.replaceFirst(numberValue, EMPTY).replaceFirst(COMMA, EMPTY);
                    yield parseNumberValue(numberValue, valueType);
                }
            }; */
            map.put(keyInstance, valueInstance);
        }


        System.out.println("json map:" + json);
        return map;
    }

    private Object parseObjectForMap(String json, Class<?> type) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException {
        char startSymbol=json.charAt(0);
        return switch (startSymbol) {
            case '"': {
                System.out.println("String parsing before gson="+json);
                String stringValue = extractStringValue(json);
                json=json.replaceFirst(stringValue, EMPTY);
                stringValue = stringValue.substring(1, stringValue.length() - 1);
                System.out.println("stringParsing gson=" + json);
                yield parseStringValue(stringValue, type);
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
                yield parseNumberValue(numberValue, type);
            }
        };
    }

    private String extractStringValue(String json) {
        Deque<Character> delimitersStack = new LinkedList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : json.toCharArray()) {
            stringBuilder.append(character);
            if (character.equals('"')) {
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
            if (character.equals(',')) {
                break;
            }
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }


}
