package util;

import java.util.Scanner;

public class IOUtil {
    public static String readInput() {
        final Scanner in = new Scanner(System.in);

        return in.nextLine();
    }

    public static String readInput(String message, Object... args) {
        System.out.printf(message, args);

        return readInput();
    }
}
