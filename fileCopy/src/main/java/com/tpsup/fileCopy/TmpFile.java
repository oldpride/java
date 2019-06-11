package com.tpsup.fileCopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;

public class TmpFile {
    public static String createTmpFile(String baseDir, String prefix, HashMap<String, String> opt) {
        String username = System.getProperty("user.name");
        String yyyyMMdd = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
        String HHmmss = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
        if (baseDir == null) {
            baseDir = System.getProperty("user.home");
        }
        baseDir = baseDir.replace("W", "/");
        String rootString = baseDir + "/tmp_" + username;
        File rootDir = new File(rootString);
        if (!rootDir.isDirectory()) {
            rootDir.mkdirs();
        }
        String tmpDirString = rootString + "/" + yyyyMMdd;
        File tmpDirFile = new File(tmpDirString);
        String tmpFileString = null;
        if (!tmpDirFile.isDirectory()) {
            tmpDirFile.mkdirs();
            // clean old files first
            File[] list = rootDir.listFiles();
            if (list == null)
                return tmpFileString;
            long now_ms = System.currentTimeMillis();
            long max_ms = 1 * 24 * 60 * 60 * 1000; // only save for one day
            for (File f : list) {
                if (now_ms - f.lastModified() > max_ms) {
                    System.out.println("removing " + f + "\n");
                    try {
                        if (f.isDirectory()) {
                            FileUtils.deleteDirectory(f);
                        } else {
                            FileUtils.deleteQuietly(f);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (opt.containsKey("chkSpace")) {
            long requiredSpace = Integer.parseInt(opt.get("chkSpace"));
            if (requiredSpace > 0) {
                FileStore store = null;
                long available = 0;
                try {
                    store = Files.getFileStore(Paths.get(tmpDirString));
                    available = store.getUsableSpace();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                if (available < requiredSpace) {
                    System.err.println(tmpDirString + " only has " + available + " bytes < required "
                            + requiredSpace + " bytes" + "\n");
                    return null;
                }
                if (opt.containsKey("verbose")) {
                    System.err.println(tmpDirString + " has " + available + " bytes >= required "
                            + requiredSpace + " bytes" + "\n");
                }
            }
        }
        tmpFileString = tmpDirString + "/" + prefix + "_" + HHmmss;
        return tmpFileString;
    }
}
