package com.tpsup.queryLotsExecId;

import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;

public class DbQuery {
    static HashMap<String, Connection> connectionByUrl = new HashMap<String, Connection>();

    public static Connection getJdbcConnection(String url) {
        if (connectionByUrl.containsKey(url)) {
            return connectionByUrl.get(url);
        }
        // https://sourceforge.net/p/jtds/discussion/104389/thread/a8d2f588/
        if (url.contains("sqlserver")) {
            // This is sybase. need to load special driver
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
            } catch (java.lang.ClassNotFoundException e) {
                System.err.print("ClassNotFoundException:");
                System.err.println(e.getMessage());
            }
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn == null) {
                System.out.println("failed to connect");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connectionByUrl.put(url, conn);
        return conn;
    }

    public static void closeJdbcConnection(String url) {
        if (!connectionByUrl.containsKey(url)) {
            System.out.println("jdbc connection is already closed or never opened");
        } else {
            Connection conn = connectionByUrl.get(url);
            if (url != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            connectionByUrl.remove(url);
        }
    }

    public static HashMap<String, Object> query(String url, String sql, HashMap<String, Object> opt) {
        // url is jdbc string
        // sql is the query statement
        // opt is optional setting.
        // args: an ArrayList of arguments passed to the sql statement/procedure
        HashMap<String, Object> qr = new HashMap<String, Object>();
        ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
        ArrayList<String> columns = new ArrayList<String>();
        // https://www.codejava.net/java-se/jdbc/connect-to-oracle-database-via-jdbc
        Connection conn = getJdbcConnection(url);
        if (conn == null) {
            System.out.println("failed to connect");
            qr.put("error", "failed to connect");
            return qr;
        }
        try {
            PreparedStatement preStatement = conn.prepareStatement(sql);
            if (opt != null && opt.containsKey("args")) {
                ArrayList<String> args = (ArrayList<String>) opt.get("args");
                for (int i = 0; i < args.size(); i++) {
                    preStatement.setString(i + 1, args.get(i));
                }
            }
            if (!opt.isEmpty() && opt.containsKey("nonQuery") && opt.get("nonQuery").equals(true)) {
                preStatement.execute();
            } else {
                ResultSet rs = preStatement.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                // The column count starts from 1
                for (int i = 1; i <= columnCount; i++) {
                    String name = rsmd.getColumnName(i);
                    columns.add(name);
                }
                while (rs.next()) {
                    ArrayList<String> row = new ArrayList<String>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getString(i));
                    }
                    rows.add(row);
                }
            }
            if (preStatement != null) {
                preStatement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            qr.put("error", e.toString());
            return qr;
        }
        qr.put("columns", columns);
        qr.put("rows", rows);
        return qr;
    }

    public static void main(String[] args) {
        {
            // sybase
            String url = "jdbc:jtds:sqlserver://sybasel.abc.com:4100;tds=5.();user=syb_user;password=hello;servertype=2";
            HashMap<String, Object> opt = new HashMap<String, Object>();
            String sql = "select name, type\r\n" + "from sysobjects o\r\n" + "where type = 'U'\r\n"
                    + "order by name\r\n";
            HashMap<String, Object> qr = query(url, sql, opt);
            if (!qr.containsKey("error")) {
                System.out.println(StringUtils.join(qr.get("columns"), ", "));
                for (ArrayList<String> row : (ArrayList<ArrayList<String>>) qr.get("rows")) {
                    System.out.println(StringUtils.join(row, ","));
                }
            } else {
                System.out.println(qr.get("error"));
            }
        }
        {
            // oracle with service name
            String url = "jdbc:oracle:thin:ORACLE_USER/temp#pass@//oraclel.abc.com:1625/oraclel.abc.com";
            HashMap<String, Object> opt = new HashMap<String, Object>();
            String sql = "select * from all_synonyms";
            HashMap<String, Object> qr = query(url, sql, opt);
            if (!qr.containsKey("error")) {
                System.out.println(StringUtils.join(qr.get("columns"), " , "));
                for (ArrayList<String> row : (ArrayList<ArrayList<String>>) qr.get("rows")) {
                    System.out.println(StringUtils.join(row, ", "));
                }
            } else {
                System.out.println(qr.get("error"));
            }
        }
        {
            // oracle with sql argument
            String url = "jdbc:oracle:thin:ORACLE_USER/temp#pass@//oraclel.abc.com:1625/oraclel.abc.com";
            HashMap<String, Object> opt = new HashMap<String, Object>();
            String sql = "select * from all_synonyms where OWNER = ?";
            ArrayList<String> sql_args = new ArrayList<String>();
            sql_args.add("SYSTEM");
            opt.put("args", sql_args);
            HashMap<String, Object> qr = query(url, sql, opt);
            System.out.println(StringUtils.join(qr.get("columns"), " , "));
            for (ArrayList<String> row : (ArrayList<ArrayList<String>>) qr.get("rows")) {
                System.out.println(StringUtils.join(row, ", "));
            }
        }
    }
}
