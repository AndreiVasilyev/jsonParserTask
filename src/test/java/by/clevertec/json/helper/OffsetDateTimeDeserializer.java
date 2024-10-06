package by.clevertec.json.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static by.clevertec.json.helper.StringData.DATETIME_OFFSET_CUSTOM_PATTERN;

public class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER
            = DateTimeFormatter.ofPattern(DATETIME_OFFSET_CUSTOM_PATTERN);

    @Override
    public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String dateAsString = jsonParser.getText();
        if (dateAsString == null) {
            throw new IOException("OffsetDateTime argument is null.");
        }
        return OffsetDateTime.parse(dateAsString, DATE_TIME_FORMATTER);
    }

}
