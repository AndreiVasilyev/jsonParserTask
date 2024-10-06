package by.clevertec.json.helper;

public class StringConstants {

    private StringConstants() {
    }

    public static final String FIELD_START_TEMPLATE = "\\\"%s\\\":.";
    public static final String FIELD_STRING_TEMPLATE = "\\\"%s\\\":\\\"[^\\\"]+\\\"[,]?";
    public static final String FIELD_ARRAY_TEMPLATE = "\\\"%s\\\":\\[.+\\]";
    public static final String FIELD_DIGITS_TEMPLATE="\\\"%s\\\":\\d+[\\.]?\\d+[,]?";
    public static final String FIELD_OBJECT_TEMPLATE="\\\"%s\\\":\\{.+";
    public static final String EMPTY = "";
    public static final String QUOTE = "\"";
    public static final String COMMA = ",";
    public static final String SPACE = " ";
    public static final String NEWLINE = "\n";
    public static final String TAB = "\t";
    public static final String COLON = ":";
    public static final char OPEN_QUOTE = '"';
    public static final char OPEN_CURLY_BRACE= '{';
    public static final char OPEN_SQUARE_BRACKET = '[';
    public static final char CLOSE_CURLY_BRACE= '}';
    public static final char CLOSE_SQUARE_BRACKET = ']';
    public static final char COMMA_CHAR = ',';
    public static final String PARSE_METHOD_NAME = "parse";
    public static final String VALUE_OF_METHOD_NAME = "valueOf";


}
