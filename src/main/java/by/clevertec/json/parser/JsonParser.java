package by.clevertec.json.parser;


import by.clevertec.json.exception.JsonSyntaxException;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
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
            System.out.println("found map:" + clazz.getTypeName());
            System.out.println(Arrays.toString(clazz.getGenericInterfaces()));
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
                        if (fieldClazz.equals(OffsetDateTime.class)) {
                            Method method = fieldClazz.getMethod("parse", CharSequence.class, DateTimeFormatter.class);
                            Object fieldInstance = method.invoke(null, fieldValue, dateTimeFormatter);
                            setFieldValue(declaredField, currentObject, fieldInstance);
                        } else if (fieldClazz.equals(String.class)) {
                            setFieldValue(declaredField, currentObject, fieldValue);
                        } else {
                            try {
                                Object fieldInstance = fieldClazz.getDeclaredConstructor(String.class).newInstance(fieldValue);
                                setFieldValue(declaredField, currentObject, fieldInstance);
                            } catch (NoSuchMethodException e) {
                                Method[] declaredMethods = fieldClazz.getDeclaredMethods();
                                boolean isExistMethod = false;
                                for (Method method : declaredMethods) {
                                    Class<?>[] typeParameters = method.getParameterTypes();
                                    Class<?> returnType = method.getReturnType();
                                    if (typeParameters.length == 1
                                            && typeParameters[0].isAssignableFrom(String.class)
                                            && returnType.isAssignableFrom(fieldClazz)
                                            && Modifier.isPublic(method.getModifiers())) {
                                        Object fieldInstance = method.invoke(null, fieldValue);
                                        setFieldValue(declaredField, currentObject, fieldInstance);
                                        isExistMethod = true;
                                        break;
                                    }
                                }
                                if (!isExistMethod)
                                    throw new JsonSyntaxException("Invalid JSON syntax when parsing object field");
                            }
                        }
                        break;
                    }
                    case "{": {

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
                        if (Character.isDigit(symbol.charAt(0))) {
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
                            Method method;
                            if (fieldClazz.isPrimitive()) {
                                String methodName = "parse" + fieldClazz.getSimpleName();
                                method = fieldClazz.getMethod(methodName, String.class);
                            } else {
                                method = fieldClazz.getMethod("valueOf", String.class);
                            }
                            Object fieldInstance = method.invoke(null, fieldValue);
                            setFieldValue(declaredField, currentObject, fieldInstance);
                        }
                }
            }
        }
        System.out.println("json=" + json);
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

}
