package util;

public class Assert {
    public static void True(Boolean assertion, String message) {
        assert assertion : message;

        if(!assertion) throw new IllegalStateException(message);
    }
}
