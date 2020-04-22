package com.tpsup.tpdist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

public class FileWalker {
    public HashMap<String, HashMap> tree;
    public String front;
    public String back;

    public FileWalker(String front, String back) {
        this.tree = new HashMap<String, HashMap>();
        this.front = front;
        this.back = back;
    }

    public void walk(String path, String relative, int level) throws IOException {
        File f = new File(path);
        HashMap<String, String> kv = new HashMap<String, String>();
        if (Files.isSymbolicLink(f.toPath())) {
            kv.put("type", "link");
            kv.put("mode", "0777");
            kv.put("size", "22"); // hardcoded for sym link
            kv.put("test", f.toPath().toRealPath().toString());
            kv.put("mtime", String.valueOf(f.lastModified() / 1000));
            kv.put("front", this.front);
            kv.put("back", this.back);
            this.tree.put(relative, kv);
            MyLog.append(MyLog.VERBOSE, String.format("Symbolic link: %s", f));
        } else if (Files.isRegularFile(f.toPath())) {
            kv.put("type", "file");
            kv.put("mode", "0644");
            kv.put("size", String.valueOf(Files.size(f.toPath())));
            kv.put("mtime", String.valueOf(f.lastModified() / 1000));
            kv.put("front", this.front);
            kv.put("back", this.back);
            this.tree.put(relative, kv);
            MyLog.append(MyLog.VERBOSE, String.format("Regular file: %s", f));
        } else if (f.isDirectory()) {
            kv.put("type", "dir");
            kv.put("mode", "0755");
            kv.put("size", "128"); // hard coded for directory
            kv.put("test", "dir");
            kv.put("mtime", String.valueOf(f.lastModified() / 1000));
            kv.put("front", this.front);
            kv.put("back", this.back);
            this.tree.put(relative, kv);
            MyLog.append(MyLog.VERBOSE, "Dir: " + relative);
            level--;
            if (level < 0) {
            	MyLog.append(MyLog.VERBOSE, "Dir: " + relative + " too deep to parse");
                return;
            }
            String[] list = f.list();
            if (list == null)
                return;
            for (String shortname : list) {
                walk(path + "/" + shortname, relative + "/" + shortname, level);
            }
        } else {
        	MyLog.append(MyLog.VERBOSE, String.format("Other: %s \n", f));
        }
    }
}
