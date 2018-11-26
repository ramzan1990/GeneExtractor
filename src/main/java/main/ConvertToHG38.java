package main;

import com.dnastack.beacon.converter.liftover.api.LiftOver;
import com.dnastack.beacon.converter.liftover.ucsc.UCSCLiftOver;
import com.dnastack.beacon.converter.util.GenomeBuild;
import htsjdk.samtools.util.Interval;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.Iterator;

import static util.Common.parseIntArray;

public class ConvertToHG38 {

    //Example Parameters: 11DG0268_SNP_Indel_ANNO.xlsx 11DG0268_SNP_Indel_ANNO_HG38.xlsx 1
    public static void main(String[] a){
        String input = a[0];
        String output = a[1];
        if(a[2].equals("1")){
            convertXLSX(input, output);
        }else{
            convertTXT(input, output);
        }
    }

    public static void convertTXT(String input, String output) {
        try {
            LiftOver intervalLiftOver = new UCSCLiftOver(GenomeBuild.HG19, GenomeBuild.HG38);
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

    public static void convertXLSX(String input, String output){
        try {
            int e = 0;
            LiftOver intervalLiftOver = new UCSCLiftOver(GenomeBuild.HG19, GenomeBuild.HG38);
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

}
