package main;

import util.Region;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class SplitJunctionsInto48 {
    private static int minCount = 10;
    public static HashMap<String, ArrayList<Region>> codingRegions = new HashMap<>();

    //Example parameters: junctions junctions_s gencode.v25.coding.genes.gtf 10
    public static void main (String[] args) {
        String input = args[0];
        String output = args[1];
        parseGTF(args[2]);
        minCount = Integer.parseInt(args[3]);
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdir();
        }
        //Count number of lines in input to report progress
        long lineCount = 0;
        try {
            Path path = Paths.get(input);
            lineCount = Files.lines(path).count();
        } catch (Exception e) {
        }
        System.out.println("Number of lines in input file: " + lineCount);
        int sl = 0;
        int sl13 = 0;
        int al13 = 0;
        int pr = -1;
        int l = 0;
        int start = -1;
        String chr = null;
        String strand = null;
        String entry = null;
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    //get values from a line
                    String[] values = line.split("\t");
                    String nChr = values[1].trim();
                    int nStart = Integer.parseInt(values[2]);
                    int nEnd = Integer.parseInt(values[3]);
                    Region nRegion = new Region(nStart, nEnd);
                    String nStrand = values[5].trim();
                    String[] samples = values[11].split(",");

                    boolean enoughCount = false;
                    StringBuilder nv = new StringBuilder();
                    for (String sample : samples) {
                        if (sample.trim().length() == 0) {
                            continue;
                        }
                        int count = Integer.parseInt(sample.split(":")[1]);
                        if (count >= minCount) {
                            enoughCount = true;
                            nv.append(",");
                            nv.append(sample);
                        }
                    }
                    String newLine = line.replace(values[11], nv.toString());
                    if (enoughCount) {
                        if (isInteger(nChr.substring(3)) || nChr.toLowerCase().equals("chrx") || nChr.toLowerCase().equals("chry")) {
                            if (chr.equals("chr13")) {
                                al13++;
                            }
                            if (nRegion.isContainedIn(codingRegions.get(nChr + nStrand))) {
                                try (PrintWriter out = new PrintWriter(new BufferedWriter
                                        (new FileWriter(output + File.separator + nChr + nStrand, true)))) {
                                    out.println(newLine);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                sl++;
                                if (chr.equals("chr13")) {
                                    sl13++;
                                }
                            }
                        }
                    }
                    if (start == -1) { //case when this is the first ever line read
                        start = nStart;
                        chr = nChr;
                        strand = nStrand;
                        entry = line;
                    } else {
                        if (strand.equals(nStrand) && chr.equals(nChr) && nStart < start) {
                            System.out.println("Not sorted (start)" + l);
                            System.out.println(entry);
                            System.out.println(line);
                            System.out.println();
                        }
                        try {
                            if (strand.equals(nStrand) && Integer.parseInt(chr.substring(3)) > Integer.parseInt(nChr.substring(3))) {
                                System.out.println("Not sorted (chr)");
                            }
                        } catch (Exception e) {

                        }
                        chr = nChr;
                        strand = nStrand;
                        start = nStart;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                l++;
                int r = (int) Math.round(((double) l / lineCount) * 100);
                if (r % 5 == 0 && pr != r) {
                    System.out.print(r + "%   ");
                    pr = r;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Total Removed lines " + sl);
        System.out.println("Removed lines in chr13 " + sl13 + " out of " + al13);
    }

    public static void parseGTF(String input) {
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    //get values from a line
                    String[] values = line.split("\t");
                    String nChr = values[0].trim();
                    int nStart = Integer.parseInt(values[3]);
                    int nEnd = Integer.parseInt(values[4]);
                    String nStrand = values[6].trim();
                    String info = values[8];
                    if (info.contains("protein_coding")) {
                        Region nRegion = new Region(nStart, nEnd);
                        if (codingRegions.containsKey(nChr + nStrand)) {
                            codingRegions.get(nChr + nStrand).add(nRegion);
                        } else {
                            ArrayList<Region> rl = new ArrayList<>();
                            rl.add(nRegion);
                            codingRegions.put(nChr + nStrand, rl);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }
}
