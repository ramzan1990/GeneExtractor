package main;

import com.dnastack.beacon.converter.liftover.api.LiftOver;
import com.dnastack.beacon.converter.liftover.exception.LiftOverException;
import com.dnastack.beacon.converter.liftover.ucsc.UCSCLiftOver;
import com.dnastack.beacon.converter.util.GenomeBuild;
import htsjdk.samtools.util.Interval;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class Main {
    public static final String MISSING = "missing";
    public static ArrayList<String> cList = new ArrayList<>();
    public static int cn = 0, maxCS = 0, minCount = 5;
    public static HashMap<String, ArrayList<Region>> codingRegions = new HashMap<>();
    public static HashMap<String, ArrayList<Region>> ROHMap = new HashMap<>();
    public static HashMap<String, ArrayList<Integer>> unannotatedVariants = new HashMap<>();
    public static HashMap<String, ArrayList<Integer>> alleleFrequency = new HashMap<>();
    private static int tchr48 = 0;
    private static ExecutorService pool;


    public static void main(String[] args) {
        //f2(new String[]{"11DG0268_SNP_Indel_ANNO.xlsx", "gene-symbol-ensembl-mapping.tsv", "out.txt"});
        //step1(new String[]{"roh.txt", "gencode.v19.annotation.gtf_withproteinids", "id_map.txt", "out_step1.csv"});
        //step1(new String[]{"roh.txt", "gencode.v19.annotation.gtf_withproteinids", "out_step1.txt"}, false);
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

        if (args[0].equals("a")) {
            parseGTF(new String[]{"gencode.v25.coding.genes.gtf"});
            checkAndSplitJunctions(new String[]{"junctions", "junctions_s"});
        } else if (args[0].equals("b")) {
            parseJunctionsFolder(new String[]{"junctions_s", "junctions_o"});
        } else if (args[0].equals("c")) {
            File cdirectory = new File("clusters");
            if (!cdirectory.exists()) {
                cdirectory.mkdir();
            }
            //parseJunctions(new String[]{"C:\\Users\\Jumee\\Desktop\\chr13-", "chr13-_output", "chr13-"});
            parseJunctions(new String[]{"junctions_s" + File.separator + "chr13-", "output13", "chr13-"});
            parseJunctions(new String[]{"junctions_s" + File.separator + "chr13+", "output13", "chr13+"});
        } else if (args[0].equals("d")) {
            convertXLSX(new String[]{"11DG0268_SNP_Indel_ANNO.xlsx", "11DG0268_SNP_Indel_ANNO_HG38.xlsx"});
            convertTXT(new String[]{"roh.txt", "roh_HG38.txt"});
        } else if (args[0].equals("e")) {
            parseROH("roh_HG38.txt");
            parseExcelForFiltering(new String[]{"11DG0268_SNP_Indel_ANNO_HG38.xlsx"});
            parseJunctions2(new String[]{"chr13+.tsv", "filtering" + File.separator + "ch13+_0.tsv", "chr13", "0"});
            parseJunctions2(new String[]{"filtering" + File.separator + "ch13+_0.tsv", "filtering" + File.separator + "ch13+_1.tsv", "chr13", "1"});
            parseJunctions2(new String[]{"filtering" + File.separator + "ch13+_1.tsv", "filtering" + File.separator + "ch13+_2.tsv", "chr13", "2"});
        }

        //parseJunctions(new String[]{"junctions-", "out_junctions-"});
        //parseJunctions(new String[]{"C:\\Users\\Jumee\\Desktop\\junctions", "out_junctions-"});
    }

    public static void convertTXT(String[] args) {
        try {
            LiftOver intervalLiftOver = new UCSCLiftOver(GenomeBuild.HG19, GenomeBuild.HG38);
            String input = args[0];
            String output = args[1];
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(input))) {
                for (String line; (line = br.readLine()) != null; ) {
                    try {
                        String[] values = line.split(":");
                        int[] r = parseIntArray(values[1].split("-"));
                        Interval interval = new Interval(values[0], r[0], r[1]);
                        Interval newInterval = intervalLiftOver.liftOver(interval, 0.91);
                        sb.append(newInterval.getContig());
                        sb.append(":");
                        sb.append(newInterval.getStart());
                        sb.append("-");
                        sb.append(newInterval.getEnd());
                        sb.append(System.lineSeparator());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try (PrintWriter out = new PrintWriter(new BufferedWriter
                    (new FileWriter(output, false)))) {
                out.print(sb.toString().trim());
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convertXLSX(String[] args) {
        try {
            int e = 0;
            LiftOver intervalLiftOver = new UCSCLiftOver(GenomeBuild.HG19, GenomeBuild.HG38);
            String input = args[0];
            String output = args[1];
            Workbook wb = WorkbookFactory.create(new File(input));
            Sheet sheet = wb.getSheetAt(0);
            Row row;
            int rows = sheet.getPhysicalNumberOfRows();
            for (int r = 1; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    try {
                        String chr = row.getCell(0).getStringCellValue();
                        chr = chr.replaceAll("chr0", "chr");
                        int start = (int) row.getCell(1).getNumericCellValue();
                        int end = (int) row.getCell(2).getNumericCellValue();
                        Interval interval = new Interval(chr, start, end);
                        Interval newInterval = intervalLiftOver.liftOver(interval, 0.91);
                        row.getCell(1).setCellValue(newInterval.getStart());
                        row.getCell(2).setCellValue(newInterval.getEnd());
                    } catch (Exception ex) {
                        e++;
                        CellStyle style = wb.createCellStyle();
                        style.setFillForegroundColor(IndexedColors.RED.getIndex());
                        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
                        Cell cell = row.getCell(18);
                        if (cell == null) {
                            cell = row.createCell(18);
                        }
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        cell.setCellValue("error");
                        Iterator<Cell> cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()) {
                            Cell c = cellIterator.next();
                            c.setCellStyle(style);
                        }
                    }
                }
            }
            FileOutputStream out = new FileOutputStream(output);
            wb.write(out);
            out.close();
            System.out.println("total rows not converted: " + e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseGTF(String[] args) {
        String input = args[0];
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

    private static void checkAndSplitJunctions(String[] args) {
        String input = args[0];
        String output = args[1];
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

    public static void parseJunctionsFolder(String[] args) {
        String input = args[0];
        String output = args[1];
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File cdirectory = new File("clusters");
        if (!cdirectory.exists()) {
            cdirectory.mkdir();
        }
        File dir = new File(input);
        File[] directoryListing = dir.listFiles();
        pool = Executors.newFixedThreadPool(4);
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().startsWith("chr")) {
                    Runnable r = new Runnable() {
                        public void run() {
                            parseJunctions(new String[]{child.getAbsolutePath(), output, child.getName()});
                        }
                    };
                    pool.execute(r);
                }
            }
        }
    }

    public static void parseJunctions(String[] args) {
        String input = args[0];
        String output = args[1] + File.separator + args[2];
        String chrFile = args[2];
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String chr = null;
        int clusterStart = -1;
        int clusterEnd = -1;
        int commonRegionStart = -1;
        int commonRegionEnd = -1;
        HashMap<String, Sample> samplesList = new HashMap<>();
        String clustersFile = "clusters" + File.separator + chrFile;

        //Count number of lines in input to report progress
        //long lineCount = 0;
        //try {
        //Path path = Paths.get(input);
        //lineCount = Files.lines(path).count();
        // } catch (Exception e) {
        // }
        //System.out.println("Number of lines in input file(" + input + "): " + lineCount);
        //int pr = -1;
        //int l = 0;
        //int cs = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    //get values from a line
                    String[] values = line.split("\t");
                    String currentLineChr = values[1];
                    int currentLineStart = Integer.parseInt(values[2]);
                    int currentLineEnd = Integer.parseInt(values[3]);
                    String[] samples = values[11].split(",");
                    String junctionID = currentLineChr.substring(3) + ":" + currentLineStart + ":" + currentLineEnd;
                    if (clusterStart == -1) { //case when this is the first ever line read
                        clusterStart = currentLineStart;
                        clusterEnd = currentLineEnd;
                        commonRegionStart = currentLineStart;
                        commonRegionEnd = currentLineEnd;
                        chr = currentLineChr;
                        parseSamples(junctionID, samplesList, samples);
                    } else if (!(commonRegionEnd <= currentLineStart)) { //case when the read can be continued
                        parseSamples(junctionID, samplesList, samples);
                        //extending end
                        if (currentLineEnd > clusterEnd) {
                            clusterEnd = currentLineEnd;
                        }
                        if (commonRegionStart < currentLineStart) {
                            commonRegionStart = currentLineStart;
                        }
                        if (commonRegionEnd > currentLineEnd) {
                            commonRegionEnd = currentLineEnd;
                        }

                    } else { //case when read is stopped and new one is started
                        StringBuilder clusterID = new StringBuilder();
                        clusterID.append(chr.substring(3));
                        clusterID.append(":");
                        clusterID.append(commonRegionStart);
                        clusterID.append(":");
                        clusterID.append(commonRegionEnd);
                        saveSamples(clusterID.toString(), samplesList, output);
                        IO.writeToFile(clusterID.toString(), clustersFile);

                        samplesList.clear();
                        parseSamples(junctionID, samplesList, samples);
                        clusterStart = currentLineStart;
                        clusterEnd = currentLineEnd;
                        commonRegionStart = currentLineStart;
                        commonRegionEnd = currentLineEnd;
                        chr = currentLineChr;
                        //cs = 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //l++;
                //int r = (int) Math.round(((double) l / lineCount) * 100);
                //if (r % 10 == 0 && pr != r) {
                //    System.out.println(r + "%   Complete for " + input);
                //    pr = r;
                //}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //to deal with file ending
        StringBuilder clusterID = new StringBuilder();
        clusterID.append(chr.substring(3));
        clusterID.append(":");
        clusterID.append(commonRegionStart);
        clusterID.append(":");
        clusterID.append(commonRegionEnd);
        saveSamples(clusterID.toString(), samplesList, output);
        IO.writeToFile(clusterID.toString(), clustersFile);
        cn++;
        //double avg = 0;
        //for (Sample s : samplesList.values()) {
        //    avg += s.junctions.size();
        //}
        //avg /=samplesList.values().size();
        //sizes.add(avg);
        //System.out.println(cn + "clusters added");
        System.out.println(chrFile);
        synchronized (Main.class) {
            tchr48++;
            if (tchr48 == 48) {
                System.out.println("Finished parsing the junctions folder.");
                pool.shutdown();
                System.out.println("Flushing");
                IO.flush();
                System.out.println("All done!");
                System.exit(0);
            }
        }
    }

    private static void saveSamples(String clusterID, HashMap<String, Sample> samplesList, String output) {
        for (Sample sample : samplesList.values()) {
            StringBuilder line = new StringBuilder();
            line.append(clusterID);
            line.append("\t");
            line.append(sample.toString());
            line.append("\t");
            line.append(sample.count);
            line.append("\n");
            StringBuilder file = new StringBuilder();
            file.append(output);
            file.append(File.separator);
            file.append(sample.id);
            IO.writeToFile(line.toString(), file.toString());
        }
    }

    private static void parseSamples(String jid, HashMap<String, Sample> samplesList, String[] samples) {
        for (String sample : samples) {
            if (sample.trim().length() <= 0) {
                continue;
            }
            int count = Integer.parseInt(sample.split(":")[1]);
            String id = sample.split(":")[0];
            Sample s;
            if (samplesList.containsKey(id)) {
                s = samplesList.get(id);
                s.count += count;
            } else {
                s = new Sample(id, count);
                samplesList.put(id, s);
            }
            StringBuilder junctionID = new StringBuilder();
            junctionID.append(jid);
            junctionID.append(":");
            junctionID.append(count);
            s.junctions.add(junctionID.toString());
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

    public static void step1(String[] args) {
        //"roh.txt", "gencode.v19.annotation.gtf_withproteinids", "id_map.txt", "out_step1.csv"
        String input1 = args[0];
        String input2 = args[1];
        String outputMapping = args[2];
        String outputSelectedGenes = args[3];

        //parse the gtf file
        StringBuilder IDMappingSB = new StringBuilder();
        StringBuilder selectedGenesSB = new StringBuilder();
        ArrayList<String> selectedGenes = new ArrayList<>();
        ArrayList<String> allGenes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(input2))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    String[] values = line.split("\t");
                    //select lines that start with "chr" and their type is "transcript"
                    if (values[0].toLowerCase().startsWith("chr") && values[2].equals("transcript")) {
                        //create mapping for entries properties
                        HashMap<String, String> infoMap = new HashMap<>();
                        String[] info = values[8].split(";");
                        for (String s : info) {
                            s = s.trim();
                            String key = s.split("\\s+")[0];
                            String val = s.split("\\s+")[1];
                            if (val.startsWith("\"") && val.endsWith("\"")) {
                                val = val.substring(1, val.length() - 1);
                            }
                            infoMap.put(key, val);
                        }
                        String eID = infoMap.get("gene_id").split("\\.")[0];
                        String tID = infoMap.get("transcript_id").split("\\.")[0];
                        //record all the unique genes mapping from gene id to transcript id
                        if (!allGenes.contains(eID)) {
                            allGenes.add(eID);
                            IDMappingSB.append(eID);
                            IDMappingSB.append(",");
                            IDMappingSB.append(tID);
                            IDMappingSB.append(System.lineSeparator());
                        }
                        //record the protein coding genes
                        if (infoMap.get("gene_type").equals("protein_coding")) {
                            //which intersect regions from roh
                            if (intersects(new int[]{Integer.parseInt(values[3]), Integer.parseInt(values[4])}, ROHMap.get(values[0]))) {
                                if (!selectedGenes.contains(eID)) {
                                    selectedGenesSB.append(eID);
                                    selectedGenesSB.append(System.lineSeparator());
                                    selectedGenes.add(eID);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Unique genes: " + allGenes.size() + ", selected genes: " + selectedGenes.size());

        //write to file the mapping from gene id to transcript id
        try (PrintWriter out = new PrintWriter(outputMapping)) {
            out.print(IDMappingSB.toString().trim());
        } catch (Exception e) {

            e.printStackTrace();
        }

        try (PrintWriter out = new PrintWriter(outputSelectedGenes)) {
            out.print(selectedGenesSB.toString().trim());
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void step2(String args[]) {
        //"11DG0268_SNP_Indel_ANNO.xlsx", "gene-symbol-ensembl-mapping.tsv", "out.txt"
        String excelFile = args[0];
        String geneMapping = args[1];
        String output = args[2];

        //parse file with gene-symbol-ensembl-mapping
        HashMap<String, String> nameMapping = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(geneMapping))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    String[] values = line.split("\t");
                    String enID = "";
                    if (values.length > 8) {
                        enID = values[8].trim();
                    }
                    if (enID.length() == 0) {
                        //sometimes entry exists but ensemble id is missing
                        enID = MISSING;
                    }
                    //current symbol
                    if (values[1].length() > 0) {
                        put(nameMapping, values[1].trim(), enID);
                    }
                    //previous symbols
                    if (values.length > 4 && values[4].length() > 0) {
                        String[] prevSymbols = values[4].split(",");
                        for (String s : prevSymbols) {
                            put(nameMapping, s.trim(), enID);
                        }
                    }
                    //synonym symbols
                    if (values.length > 5 && values[5].length() > 0) {
                        String[] synonyms = values[5].split(",");
                        for (String s : synonyms) {
                            put(nameMapping, s.trim(), enID);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //used to record different types of errors
        StringBuilder sb = new StringBuilder();
        StringBuilder sbe = new StringBuilder();
        StringBuilder sbe1 = new StringBuilder();
        StringBuilder sbe2 = new StringBuilder();
        StringBuilder sbe3 = new StringBuilder();
        ArrayList<String> selectedGenes = new ArrayList<>();
        ArrayList<String> allGenes = new ArrayList<>();
        //used to count different types of errors
        int i1 = 0;
        int i2 = 0;
        int er1 = 0;
        int er2 = 0;
        int er3 = 0;
        //try to parse the excel file
        try {
            Workbook wb = WorkbookFactory.create(new File(excelFile));
            Sheet sheet = wb.getSheetAt(0);
            Row row;
            int rows = sheet.getPhysicalNumberOfRows();
            for (int r = 1; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    String type = row.getCell(5).getStringCellValue();
                    String dbSNP = row.getCell(9).getStringCellValue();
                    String dbSNP131 = row.getCell(10).getStringCellValue();
                    String dbSNP132 = row.getCell(15).getStringCellValue();
                    String genomes1000 = row.getCell(16).getStringCellValue();
                    String gene = row.getCell(12).getStringCellValue();
                    i1++;
                    if (!allGenes.contains(gene)) {
                        allGenes.add(gene);
                    }
                    //if the conditions is satisfied the gene is considered
                    if (type.equals("hom") && dbSNP.equals(".") && dbSNP131.equals(".") && dbSNP132.equals(".") && genomes1000.equals(".")) {
                        i2++;
                        if (!selectedGenes.contains(gene)) {
                            String outputID = "";
                            selectedGenes.add(gene);
                            //if the ensemble id is contained in mapping, use the value
                            if (nameMapping.containsKey(gene)) {
                                outputID = nameMapping.get(gene);
                            } else {
                                //somtimes the gene value consists of multople genes separated by comma, semicolon or dash
                                boolean found = false;
                                String[] values = gene.split("[\\,,;,-]+");
                                //in this case we test separate gene names
                                for (String v : values) {
                                    String vn = v.trim().replaceAll("\\(.*\\)", "");
                                    if (nameMapping.containsKey(vn)) {
                                        outputID = nameMapping.get(vn);
                                        found = true;
                                        if (!outputID.equals(MISSING)) {
                                            break;
                                        }
                                    }
                                }
                                //counting different types of errors
                                if (!found) {
                                    er1++;
                                    if (gene.contains("LOC")) {
                                        er2++;
                                        sbe2.append(gene);
                                        sbe2.append(System.lineSeparator());
                                    } else {
                                        sbe3.append(gene);
                                        sbe3.append(System.lineSeparator());
                                    }
                                }
                            }
                            //if mapping was found we put it into output
                            if (outputID.length() != 0 && !outputID.equals(MISSING)) {
                                sb.append(outputID);
                            } else {
                                //otherwise we put into output original value of genes from excel file
                                sb.append(gene);
                                //we also output the problem causing genes into separate files
                                sbe.append(gene);
                                sbe.append(System.lineSeparator());
                                if (outputID.equals(MISSING)) {
                                    er3++;
                                    er1++;
                                    sbe1.append(gene);
                                    sbe1.append(System.lineSeparator());
                                }
                            }
                            sb.append(System.lineSeparator());
                        }
                    }
                }
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        System.out.println("Name mappings not found: " + er1);
        System.out.println("LOC missing name mappings: " + er2);
        System.out.println("Entry exists but no ENSG name found: " + er3);
        System.out.println("Number of rows: " + i1);
        System.out.println("Rows with the condition: " + i2);
        System.out.println("Number of genes: " + allGenes.size());
        System.out.println("Number of selected genes: " + selectedGenes.size());

        try (PrintWriter out = new PrintWriter(output)) {
            out.print(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (PrintWriter out = new PrintWriter("not_found.txt")) {
            out.print(sbe.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (PrintWriter out = new PrintWriter("not_found[entry_exists].txt")) {
            out.print(sbe1.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (PrintWriter out = new PrintWriter("not_found.txt[LOC].txt")) {
            out.print(sbe2.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (PrintWriter out = new PrintWriter("not_found.txt[other].txt")) {
            out.print(sbe3.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean intersects(int[] geneLoc, ArrayList<Region> a) {
        if (a == null) {
            return false;
        }
        for (Region bounds : a) {
            if ((bounds.start <= geneLoc[0] && geneLoc[0] <= bounds.end) || (bounds.start <= geneLoc[1] && geneLoc[1] <= bounds.end)) {
                return true;
            }
        }
        return false;
    }

    private static void put(HashMap<String, String> m, String key, String value) {
        if (!m.containsKey(key) || m.get(key).equals(MISSING)) {
            m.put(key, value);
        }
    }

    public void f1(String args[]) {
        //"11DG0268_SNP_Indel_ANNO.xlsx",  "out.txt"
        String input = args[0];
        String output = args[1];
        int minValue = 2000000;
        if (args.length > 2) {
            minValue = Integer.parseInt(args[2]);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Gene\t Chromosome\t  Homozygous start locus\t Homozygous end locus\n");
        try {
            Workbook wb = WorkbookFactory.create(new File(input));
            Sheet sheet = wb.getSheetAt(0);
            Row row;
            int rows = sheet.getPhysicalNumberOfRows();

            int homs = 0;
            int hstart = 0;
            String pName = "";
            int pEnd = 0;
            ArrayList<String> hgenes = new ArrayList<String>();
            ArrayList<String> tgenes = new ArrayList<String>();
            for (int r = 1; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    int start = (int) row.getCell(1).getNumericCellValue();
                    int end = (int) row.getCell(2).getNumericCellValue();
                    String type = row.getCell(5).getStringCellValue();
                    boolean ex = row.getCell(11).getStringCellValue().toLowerCase().contains("exonic");
                    String gene = row.getCell(12).getStringCellValue();
                    boolean sn = row.getCell(13).getStringCellValue().toLowerCase().startsWith("synonymous");
                    String name = row.getCell(0).getStringCellValue();
                    //check condition
                    if (!type.equals("hom") || !(name.equals(pName) || pName.length() == 0)) {
                        int tEnd = pEnd;
                        if (homs > 0) {
                            //check length of the region
                            if (tEnd - hstart > minValue) {
                                for (String g : hgenes) {
                                    if (tgenes.contains(g)) {
                                        continue;
                                    } else {
                                        tgenes.add(g);
                                    }
                                    sb.append(g);
                                    sb.append("\t");
                                    sb.append(pName);
                                    sb.append("\t");
                                    sb.append(hstart);
                                    sb.append("\t");
                                    sb.append(tEnd);
                                    sb.append("\n");
                                }
                            }
                        }
                        homs = 0;
                        hgenes.clear();
                    }
                    if (type.equals("hom") && !name.equals("chrX")) {
                        homs++;
                        if (homs == 1) {
                            hstart = start;
                        }
                        if (ex && !sn) {
                            hgenes.add(gene);
                        }
                    }
                    pName = name;
                    pEnd = end;
                }
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        try (PrintWriter out = new PrintWriter(output)) {
            out.print(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int[] parseIntArray(String[] arr) {
        return Stream.of(arr).mapToInt(Integer::parseInt).toArray();
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

    public static void parseJunctionsFolder2(String[] args) {
        String input = args[0];
        String output = args[1];
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File dir = new File(input);
        File[] directoryListing = dir.listFiles();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().startsWith("chr")) {
                    Runnable r = new Runnable() {
                        public void run() {
                            parseJunctions2(new String[]{child.getAbsolutePath(), output, child.getName()});
                        }
                    };
                    pool.execute(r);
                }
            }
        }
    }

    public static void parseJunctions2(String[] args) {
        String input = args[0];
        String output = args[1];
        String chrFile = args[2];
        String mode = args[3];
        StringBuilder sb = new StringBuilder();
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
                        if (r.isContainedIn(ROHMap.get(chrFile))) {
                            sb.append(line);
                            sb.append("\n");
                        }
                    } else if (mode.equals("1")) {
                        if (r.isInRange(unannotatedVariants.get(chrFile), 5000)) {
                            sb.append(line);
                            sb.append("\n");
                        }
                    } else if (mode.equals("2")) {
                        if (r.isInRange(alleleFrequency.get(chrFile), 5000)) {
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
        System.out.println(chrFile + " - " + sb.toString().split("\n").length);
        try (PrintWriter out = new PrintWriter(output)) {
            out.print(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
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
}
