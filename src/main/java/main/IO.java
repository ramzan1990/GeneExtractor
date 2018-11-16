package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IO {
    public static HashMap<String, StringBuilder> pending = new HashMap<>();
    static ExecutorService pool = Executors.newFixedThreadPool(1);

    public static void writeToFile(String line, String file) {
        if(line==null){
            System.out.println(file + "--- tried to write null");
            return;
        }
        String interned = file.intern();
        synchronized (interned) {
            StringBuilder sb;
            if (pending.keySet().contains(file)) {
                sb = pending.get(file);
            } else {
                sb = new StringBuilder();
                pending.put(file, sb);
            }
            sb.append(line);
            if (sb.length() > 20000) {
                final String toSave = sb.toString();
                pending.put(file, new StringBuilder());
                Runnable r = new Runnable() {
                    public void run() {
                        try (PrintWriter out = new PrintWriter(new BufferedWriter
                                (new FileWriter(file, true)))) {
                            out.print(toSave);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                pool.execute(r);
            }
        }
    }

    public static void flush() {
        for (String file : pending.keySet()) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter
                    (new FileWriter(file, true)))) {
                out.print(pending.get(file));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pool.shutdown();
    }
}
