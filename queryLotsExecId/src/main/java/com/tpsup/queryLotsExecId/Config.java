package com.tpsup.queryLotsExecId;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    public static Properties getProperties(String filename) {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(filename);
            prop.load(input);
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop;
    }

    public static HashMap<String, String> getJdbcConnections(String filename) {
        HashMap<String, String> result = new HashMap<String, String>();
        Properties prop = getProperties(filename);
        Pattern urlPattern = Pattern.compile("^jdbc.url.(.+)");
        Set<Object> allKeys = prop.keySet();
        for (Object k : allKeys) {
            String ks = (String) k;
            Matcher urlMatcher = urlPattern.matcher(ks);
            if (urlMatcher.find()) {
                String value = prop.getProperty(ks);
                // mask out the real password with "password" before display the jdbc string
                // in GUI
                // jdbc.url.axiom_uat =
                // jdbc:oracle:thin:axiom_user/password@//nyarnlu.us.db.com:1725/nyarnlu.us.db.com";
                String nopassword = value.replaceFirst("A(jdbc.+?/).+?(@//.+)", "$1password$2");
                result.put(nopassword, value);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        HashMap<String, String> result = getJdbcConnections(
                "C:/Users/william/eclipse-workspace/queryLotsExecId/src/main/resources/configure.properties");
        System.out.println("result = " + result);
    }
}
