package main;

import util.Common;
import util.IO;

import java.io.*;
import java.util.ArrayList;

public class HTMLExtractor {
    public static void main(String[] a) {
        String input = a[0];
        String output = a[1];
        ArrayList<String[]> trapData = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    trapData.add(line.split("\t"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trapData.size(); i++) {
            String score = "NA";
            String file = "trap_data" + File.separator + trapData.get(i)[1].trim() + ".html";
            if(new File(file).exists()) {
                String f = Common.readFile(file);
                String g = trapData.get(i)[2];
                String s = f.replaceAll(".*<td>" + g + "</td>|</td>.*", "");
                s = s.substring(s.indexOf("<td>") + "<td>".length());
                if (Common.isNumeric(s)) {
                    score = s;
                }
            }
            sb.append(trapData.get(i)[0]);
            sb.append("\t");
            sb.append(trapData.get(i)[1]);
            sb.append("\t");
            sb.append(trapData.get(i)[2]);
            sb.append("\t");
            sb.append(score);
            sb.append("\n");
        }
        try (PrintWriter out = new PrintWriter(new BufferedWriter
                (new FileWriter(output, false)))) {
            out.print(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
