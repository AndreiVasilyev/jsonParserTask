package by.clevertec.json;

public class JsonSyntaxException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JsonSyntaxException() {
        super();
    }

    public JsonSyntaxException(String message) {
        super(message);
    }
}
