package main;

import util.IO;
import util.Sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClusterJunctions {
    private static int tchr48 = 0;
    private static ExecutorService pool;

    //Example Parameters: junctions_s junctions_o clusters
    public static void main(String[] a) {
        String input = a[0];
        String output = a[1];
        String clustersFolder =  a[2];
        File directory = new File(output);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File cdirectory = new File( a[2]);
        if (!cdirectory.exists()) {
            cdirectory.mkdirs();
        }
        File dir = new File(input);
        File[] directoryListing = dir.listFiles();
        pool = Executors.newFixedThreadPool(10);
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().startsWith("chr")) {
                    Runnable r = new Runnable() {
                        public void run() {
                            parseJunctions(child.getAbsolutePath(), output, child.getName(), clustersFolder);
                        }
                    };
                    pool.execute(r);
                }
            }
        }
    }

    public static void parseJunctions(String input, String output, String chrFile, String clustersFolder) {
        output = output + File.separator + chrFile;
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
        String clustersFile = clustersFolder + File.separator + chrFile;
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
                        clusterID.append("\t");
                        clusterID.append(clusterStart);
                        clusterID.append("\t");
                        clusterID.append(clusterEnd);
                        saveSamples(clusterID.toString(), samplesList, output);
                        IO.writeToFile(clusterID.toString() + "\n", clustersFile);

                        samplesList.clear();
                        parseSamples(junctionID, samplesList, samples);
                        clusterStart = currentLineStart;
                        clusterEnd = currentLineEnd;
                        commonRegionStart = currentLineStart;
                        commonRegionEnd = currentLineEnd;
                        chr = currentLineChr;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        IO.writeToFile(clusterID.toString() + "\n", clustersFile);
        System.out.println(chrFile);
        synchronized (LegacyCode.class) {
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
}
