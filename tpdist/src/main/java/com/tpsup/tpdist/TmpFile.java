package com.tpsup.tpdist;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class TmpFile {
    public static String createTmpFile(String baseDirString, String prefix, HashMap<String, Object> opt) {
        String yyyyMMdd = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
        String HHmmss = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());

        File baseDir = new File(baseDirString);
        if (!baseDir.isDirectory()) {
            baseDir.mkdirs();
        }
        String tmpDirString = baseDirString.replace("\\", "/") + "/" + yyyyMMdd;
        File tmpDirFile = new File(tmpDirString);
        String tmpFileString = null;
        if (!tmpDirFile.isDirectory()) {
            tmpDirFile.mkdirs();
            // clean old files first
            File[] list = baseDir.listFiles();
            if (list == null)
                return tmpFileString;
            long now_ms = System.currentTimeMillis();
            long max_ms = 1 * 24 * 60 * 60 * 1000; // only save for 1 days
            for (File f : list) {
                if (now_ms - f.lastModified() > max_ms) {
                    MyLog.append("removing " + f);
                    try {
                        if (f.isDirectory()) {
                            FileUtils.deleteDirectory(f);
                        } else {
                            FileUtils.deleteQuietly(f);
                        }
                    } catch (IOException e) {
                    	MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        }
        if (opt.containsKey("chkSpace")) {
            long requiredSpace = Integer.parseInt((String)opt.get("chkSpace"));
            if (requiredSpace > 0) {
                FileStore store = null;
                long available = 0;
                try {
                    store = Files.getFileStore(Paths.get(tmpDirString));
                    available = store.getUsableSpace();
                } catch (IOException e) {
                	MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e));
                    return null;
                }
                if (available < requiredSpace) {
                	MyLog.append(MyLog.ERROR, tmpDirString + " only has " + available + " bytes < required "
                            + requiredSpace + " bytes");
                    return null;
                }
                if (opt.containsKey("verbose")) {
                	MyLog.append(tmpDirString + " has " + available + " bytes >= required "
                            + requiredSpace + " bytes");
                }
            }
        }
        
        // https://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
        String pid = (ManagementFactory.getRuntimeMXBean().getName().split("@"))[0];
        tmpFileString = tmpDirString + "/" + prefix + "_" + HHmmss + pid;
        return tmpFileString;
    }
}
