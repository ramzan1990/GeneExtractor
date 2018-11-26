package main;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import util.Region;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.Common.parseIntArray;

public class Filtering {
    public static HashMap<String, ArrayList<Region>> ROHMap = new HashMap<>();
    public static HashMap<String, ArrayList<Integer>> unannotatedVariants = new HashMap<>();
    public static HashMap<String, ArrayList<Integer>> alleleFrequency = new HashMap<>();
    public static int drange = 400;
    public static boolean dinv = false;
    private static int filesFiltered = 0;
    private static int jobs = 0;
    private static boolean finished = false;
    private static String input = null;
    private static String output = null;
    private static String roh = null;
    private static String excel = null;
    private static String mode = null;

    public static void main(String[] args) {
        for (String a : args) {
            String[] p = a.split("=");
            String pn = p[0].trim();
            String pv = p[1].trim();
            if (pn.equals("range")) {
                drange = Integer.parseInt(pv);
            } else if (pn.equals("inverse")) {
                dinv = pv.toLowerCase().equals("true");
            } else if (pn.equals("in")) {
                input = pv;
            } else if (pn.equals("out")) {
                output = pv;
            } else if (pn.equals("roh")) {
                roh = pv;
            } else if (pn.equals("excel")) {
                excel = pv;
            } else if (pn.equals("mode")) {
                mode = pv;
            }
        }
        for (int i = 1; i < 23; i++) {
            ROHMap.put("chr" + i, new ArrayList<>());
            alleleFrequency.put("chr" + i, new ArrayList<>());
            unannotatedVariants.put("chr" + i, new ArrayList<>());
        }
        ROHMap.put("chrX", new ArrayList<>());
        alleleFrequency.put("chrX", new ArrayList<>());
        unannotatedVariants.put("chrX", new ArrayList<>());

        ROHMap.put("chrY", new ArrayList<>());
        alleleFrequency.put("chrY", new ArrayList<>());
        unannotatedVariants.put("chrY", new ArrayList<>());

        parseROH(roh);
        parseExcelForFiltering(new String[]{excel});

        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File dir = new File(input);
        File[] directoryListing = dir.listFiles();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().contains("chr")) {
                    jobs++;
                    Runnable r = new Runnable() {
                        public void run() {
                            parseJunctions2(new String[]{child.getAbsolutePath(), output, child.getName(), mode});
                        }
                    };
                    pool.execute(r);
                }
            }
        }
        finished = true;
        if (jobs == filesFiltered) {
            System.exit(1);
        }
    }

    public static void parseJunctions2(String[] args) {
        String input = args[0];
        String output = args[1];
        String chrFile = args[2];
        chrFile = chrFile.split("\\.")[0];
        chrFile = chrFile.substring(0, chrFile.length() - 1);
        String mode = args[3];
        StringBuilder sb = new StringBuilder();
        long lineCount = 0;
        try {
            Path path = Paths.get(input);
            lineCount = Files.lines(path).count();
        } catch (Exception e) {
        }
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            sb.append(br.readLine());
            sb.append("\n");
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    //get values from a line
                    String[] values = line.split("\t");
                    int s = Integer.parseInt(values[0]);
                    int e = Integer.parseInt(values[1]);
                    Region r = new Region(s, e);
                    if (mode.equals("0")) {
                        if (dinv != r.isContainedIn(ROHMap.get(chrFile))) {
                            sb.append(line);
                            sb.append("\n");
                        }
                    } else if (mode.equals("1")) {
                        if (dinv != r.isInRange(unannotatedVariants.get(chrFile), drange)) {
                            sb.append(line);
                            sb.append("\n");
                        }
                    } else if (mode.equals("2")) {
                        if (dinv != r.isInRange(alleleFrequency.get(chrFile), drange)) {
                            sb.append(line);
                            sb.append("\n");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(args[2] + " done");
        System.out.println(args[2] + " - " + lineCount + " - " + sb.toString().split("\n").length);
        try (PrintWriter out = new PrintWriter(output + File.separator + args[2])) {
            out.print(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        filesFiltered++;
        if (finished && jobs == filesFiltered) {
            System.exit(1);
        }
    }

    public static void parseExcelForFiltering(String args[]) {
        String excelFile = args[0];
        //try to parse the excel file
        try {
            Workbook wb = WorkbookFactory.create(new File(excelFile));
            Sheet sheet = wb.getSheetAt(0);
            Row row;
            int rows = sheet.getPhysicalNumberOfRows();
            for (int r = 1; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    String er = "";
                    if (row.getCell(18) != null) {
                        er = row.getCell(18).getStringCellValue();
                    }
                    if (!er.equals("error")) {
                        int start = (int) row.getCell(1).getNumericCellValue();
                        String chr = row.getCell(0).getStringCellValue();
                        chr = chr.replaceAll("chr0", "chr");
                        String dbSNP = row.getCell(9).getStringCellValue();
                        String dbSNP131 = row.getCell(10).getStringCellValue();
                        String dbSNP132 = row.getCell(15).getStringCellValue();
                        String genomes1000 = row.getCell(16).getStringCellValue();
                        double allele = 0;
                        if (row.getCell(17) != null) {
                            allele = Double.parseDouble(row.getCell(17).getStringCellValue());
                        }
                        //if the conditions is satisfied the gene is considered
                        if (dbSNP.equals(".") && dbSNP131.equals(".") && dbSNP132.equals(".") && genomes1000.equals(".")) {
                            unannotatedVariants.get(chr).add(start);
                        }
                        if (allele < 0.001) {
                            alleleFrequency.get(chr).add(start);
                        }
                    }
                }
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }


    public static void parseROH(String input) {
        //parse the roh.txt file
        //map chromosomes to regions
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    String[] values = line.split(":");
                    int[] v = parseIntArray(values[1].split("-"));
                    ROHMap.get(values[0]).add(new Region(v[0], v[1]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
