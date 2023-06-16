package util;

public class AssertUtil {
    public static void check(boolean rule, String message) throws RuntimeException {
        if(!rule) throw new RuntimeException(message);
    }
}
