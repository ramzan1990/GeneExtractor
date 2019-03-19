package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.stream.Stream;

public class Common {
    public static int[] parseIntArray(String[] arr) {
        return Stream.of(arr).mapToInt(Integer::parseInt).toArray();
    }

    public static String readFile(String input){
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    sb.append(line);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  sb.toString();
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }
}
