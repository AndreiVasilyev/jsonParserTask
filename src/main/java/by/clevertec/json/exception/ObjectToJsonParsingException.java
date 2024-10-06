package by.clevertec.json.exception;

public class ObjectToJsonParsingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ObjectToJsonParsingException() {
        super();
    }

    public ObjectToJsonParsingException(String message) {
        super(message);
    }
}
