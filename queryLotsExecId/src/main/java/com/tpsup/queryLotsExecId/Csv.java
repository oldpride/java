package com.tpsup.queryLotsExecId;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class Csv {
    public static HashMap<String, ArrayList<String>> getArrayByKey(String filename, String keyColumn,
            String arrayColumn) {
        HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
        try (Reader reader = Files.newBufferedReader(Paths.get(filename))) {
            CSVParser csvParser = new CSVParser(reader,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            for (CSVRecord record : csvParser) {
                String keyValue = record.get(keyColumn).trim();
                String arrayElement = record.get(arrayColumn).trim();
                if (!result.containsKey(keyValue)) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    result.put(keyValue, arrayList);
                }
                result.get(keyValue).add(arrayElement);
            }
            reader.close();
            csvParser.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<String> getCsvHeader(String filename) {
        Reader reader = null;
        CSVParser csvParser = null;
        ArrayList<String> columns = new ArrayList<>();
        try {
            reader = Files.newBufferedReader(Paths.get(filename));
            csvParser = new CSVParser(reader,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            for (final String h : headerMap.keySet()) {
                columns.add(h);
            }
            reader.close();
            csvParser.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return columns;
    }

    public static void main(String[] args) {
        String filename = "c:/users/william/eclipse-workspace/queryLotsExecId/src/main/resources/input.csv";
        System.out.println(getCsvHeader(filename));
    }
}
