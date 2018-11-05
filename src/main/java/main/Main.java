package main;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

public class Main {
    public static final String MISSING = "missing";
    public static ArrayList<String> a1 = new ArrayList<>();
    public static int cn = 0;

    public static void main(String[] args) {
        //f2(new String[]{"11DG0268_SNP_Indel_ANNO.xlsx", "gene-symbol-ensembl-mapping.tsv", "out.txt"});
        //step1(new String[]{"roh.txt", "gencode.v19.annotation.gtf_withproteinids", "id_map.txt", "out_step1.csv"});
        //step1(new String[]{"roh.txt", "gencode.v19.annotation.gtf_withproteinids", "out_step1.txt"}, false);
        //checkAndSplitJunctions(new String[]{"junctions", "junctions_s"});
        parseJunctionsFolder(new String[]{"junctions_s", "junctions_o"});
        //parseJunctions(new String[]{"junctions-", "out_junctions-"});
        //parseJunctions(new String[]{"C:\\Users\\Jumee\\Desktop\\junctions", "out_junctions-"});
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
        int pr = -1;
        int l = 0;
        int start = -1;
        String chr = null;
        String strand = null;
        String entry = null;
        try {
            new File("junctions+").delete();
            new File("junctions-").delete();
        } catch (Exception e) {
            System.out.println("Cannot delete");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    //get values from a line
                    String[] values = line.split("\t");
                    String nChr = values[1].trim();
                    int nStart = Integer.parseInt(values[2]);
                    String nStrand = values[5].trim();
                    if (isInteger(nChr.substring(3)) || nChr.toLowerCase().equals("chrx") || nChr.toLowerCase().equals("chry")) {
                        try (PrintWriter out = new PrintWriter(new BufferedWriter
                                (new FileWriter(output + File.separator + nChr + nStrand, true)))) {
                            out.println(line);
                        } catch (Exception e) {
                            e.printStackTrace();
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
        System.out.println();
    }

    public static void parseJunctionsFolder(String[] args) {
        String input = args[0];
        String output = args[1];
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File dir = new File(input);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().startsWith("chr")) {
                    new Thread() {
                        public void run() {
                            parseJunctions(new String[]{child.getAbsolutePath(), output});

                        }
                    }.start();

                }
            }
        }
    }

    public static void parseJunctions(String[] args) {
        String input = args[0];
        String output = args[1];
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String chr = null;
        int start = -1;
        int end = -1;
        ArrayList<Sample> samplesList = new ArrayList<>();
        //Count number of lines in input to report progress
        long lineCount = 0;
        try {
            Path path = Paths.get(input);
            lineCount = Files.lines(path).count();
        } catch (Exception e) {
        }
        System.out.println("Number of lines in input file(" + input + "): " + lineCount);
        int pr = -1;
        int l = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    //get values from a line
                    String[] values = line.split("\t");
                    String nChr = values[1];
                    String nChrS = nChr.substring(3);
                    int nStart = Integer.parseInt(values[2]);
                    int nEnd = Integer.parseInt(values[3]);
                    String[] samples = values[11].split(",");
                    String junctionID = nChrS + ":" + nStart + ":" + nEnd;
                    if (start == -1) { //case when this is the first ever line read
                        start = nStart;
                        end = nEnd;
                        chr = nChr;
                        parseSamples(junctionID, samplesList, samples);
                    } else if (nStart < end) { //case when the read can be continued
                        //extending end
                        if (nEnd > end) {
                            end = nEnd;
                        }
                        parseSamples(junctionID, samplesList, samples);
                    } else { //case when read is stopped and new one is started
                        cn++;
                        if (cn % 10 == 0) {
                            System.out.println(cn + "clusters added");
                        }
                        saveSamples(chr + ":" + start + ":" + end, samplesList, output);
                        samplesList.clear();
                        parseSamples(junctionID, samplesList, samples);
                        start = nStart;
                        end = nEnd;
                        chr = nChr;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                l++;
                int r = (int) Math.round(((double) l / lineCount) * 100);
                if (r % 5 == 0 && pr != r) {
                    System.out.println(r + "%   Complete");
                    pr = r;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //to deal with file ending
        saveSamples(chr + ":" + start + ":" + end, samplesList, output);
        System.out.println();
    }

    private static void saveSamples(String clusterID, ArrayList<Sample> samplesList, String output) {
        for (Sample sample : samplesList) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter
                    (new FileWriter(output + File.separator + sample.id, true)))) {
                out.println(clusterID + "\t" + sample.toString() + "\t" + sample.count);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void parseSamples(String jid, ArrayList<Sample> samplesList, String[] samples) {
        for (String sample : samples) {
            if (sample.trim().length() <= 0) {
                continue;
            }
            int count = Integer.parseInt(sample.split(":")[1]);
            String id = sample.split(":")[0];
            Sample s;
            if (samplesList.contains(new Sample(id))) {
                s = samplesList.get(samplesList.indexOf(new Sample(id)));
                s.count += count;
            } else {
                s = new Sample(id, count);
                samplesList.add(s);
            }
            s.junctions.add(jid + ":" + count);
        }
    }

    public static void step1(String[] args) {
        //"roh.txt", "gencode.v19.annotation.gtf_withproteinids", "id_map.txt", "out_step1.csv"
        String input1 = args[0];
        String input2 = args[1];
        String outputMapping = args[2];
        String outputSelectedGenes = args[3];
        //parse the roh.txt file
        //map chromosomes to regions
        HashMap<String, ArrayList<int[]>> mapping = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(input1))) {
            for (String line; (line = br.readLine()) != null; ) {
                try {
                    String[] values = line.split(":");
                    if (mapping.containsKey(values[0])) {
                        //new chromosome case
                        mapping.get(values[0]).add(parseIntArray(values[1].split("-")));

                    } else {
                        //new region for existing chromosome
                        ArrayList<int[]> pair = new ArrayList<>();
                        pair.add(parseIntArray(values[1].split("-")));
                        mapping.put(values[0], pair);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                            if (intersects(new int[]{Integer.parseInt(values[3]), Integer.parseInt(values[4])}, mapping.get(values[0]))) {
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

    private static boolean intersects(int[] geneLoc, ArrayList<int[]> a) {
        if (a == null) {
            return false;
        }
        for (int[] bounds : a) {
            if ((bounds[0] <= geneLoc[0] && geneLoc[0] <= bounds[1]) || (bounds[0] <= geneLoc[1] && geneLoc[1] <= bounds[1])) {
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
}
