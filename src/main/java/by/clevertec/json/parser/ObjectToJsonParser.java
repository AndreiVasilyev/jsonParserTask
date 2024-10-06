package by.clevertec.json.parser;

import by.clevertec.json.exception.ObjectToJsonParsingException;

public interface ObjectToJsonParser {
    String toJson(Object obj) throws ObjectToJsonParsingException;
}
