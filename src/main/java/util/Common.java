package util;

import java.util.stream.Stream;

public class Common {
    public static int[] parseIntArray(String[] arr) {
        return Stream.of(arr).mapToInt(Integer::parseInt).toArray();
    }
}
