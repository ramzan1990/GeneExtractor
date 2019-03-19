package main;

import org.apache.commons.lang3.StringUtils;
import util.Common;
import util.IO;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

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
            if (new File(file).exists()) {
                String f = Common.readFile(file);
                String g = trapData.get(i)[2];
                String s[] = StringUtils.substringsBetween(f, "<tr class=\"text-center\">", "</tr>");
                if (s != null) {
                    for (int j = 0; j < s.length; j++) {
                        String v[] = s[j].split("<td>");
                        String pg = v[v.length - 2].replaceAll("</td>", "").trim();
                        if (g.contains(pg) || pg.contains(g) || s.length == 1) {
                            String sc = v[v.length - 1].replaceAll("</td>", "").trim();
                            if (Common.isNumeric(sc)) {
                                score = sc;
                            }
                            break;
                        }
                    }
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
