package com.tpsup.queryLotsExecId;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JTextArea;

public class Process implements Runnable {
    private String connection;
    private String sqlTemplate;
    private String csv;
    private String dateColumn;
    private String execIdColumn;
    private String output;
    private JTextArea log;

    public Process(String connection, String sqlTemplate, String csv, String dateColumn, String execIdColumn,
            String output, JTextArea parentLog, HashMap<String, HashMap> opt) {
        this.connection = connection;
        this.sqlTemplate = sqlTemplate;
        this.csv = csv;
        this.dateColumn = dateColumn;
        this.execIdColumn = execIdColumn;
        this.output = output;
        this.log = parentLog;
    }

    static private final String newline = "\n";

    public void run() {
        HashMap<String, ArrayList<String>> execIdByDate = Csv.getArrayByKey(csv, dateColumn, execIdColumn);
        log.append(execIdByDate + newline);
        ArrayList<String> sqlList = new ArrayList<>();
        // https://stackoverflow.com/questions/8962459/java-collections-keyset-vs-entryset-in-map
        for (java.util.Map.Entry<String, ArrayList<String>> entry : execIdByDate.entrySet()) {
            String date = entry.getKey();
            ArrayList<String> list = entry.getValue();
            int i = 0;
            int block = 1000; // this is the limit for sql where in () clause
            int size = list.size();
            while (i + block < size) {
                String sql = generateSQL(sqlTemplate, date, list.subList(i, i + 1000));
                sqlList.add(sql);
                i += block;
            }
            if (i < size) {
                String sql = generateSQL(sqlTemplate, date, list.subList(i, size));
                sqlList.add(sql);
            }
        }
        boolean headerPrinted = false;
        PrintWriter out = null;
        try {
            out = new PrintWriter(output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        for (String sql : sqlList) {
            HashMap<String, Object> opt = new HashMap<String, Object>();
            HashMap<String, Object> qr = DbQuery.query(connection, sql, opt);
            if (!qr.containsKey("error")) {
                System.out.println("nonQuery statement executed succssfully");
            } else {
                System.out.println("nonQuery statement failed" + qr.get("error"));
            }
            if (!qr.containsKey("error")) {
                ArrayList<ArrayList<String>> rows = (ArrayList<ArrayList<String>>) qr.get("rows");
                ArrayList<String> columns = (ArrayList<String>) qr.get("columns");
                log.append("get " + rows.size() + " \n");
                if (!headerPrinted) {
                    out.println(String.join(",", columns));
                    headerPrinted = true;
                }
                for (ArrayList<String> row : rows) {
                    out.println(String.join(",", row));
                }
            } else {
                System.out.println(qr.get("error"));
            }
        }
        out.close();
        try {
            Desktop.getDesktop().open(new File(output));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static String generateSQL(String sqlTemplate, String date, List<String> list) {
        StringBuilder bld = new StringBuilder();
        int size = list.size();
        int i = 0;
        for (String execId : list) {
            if (i < size - 1) {
                bld.append("'" + execId + "',");
            } else {
                bld.append("'" + execId + "'");
            }
            i++;
        }
        String whereInClause = bld.toString();
        sqlTemplate = sqlTemplate.replace("dateTemplate", date);
        sqlTemplate = sqlTemplate.replace("execIdTemplate", whereInClause);
        return sqlTemplate;
    }
}
