package com.tpsup.fileCopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DirTree {
    public static HashMap<String, HashMap> build_dir_tree(ArrayList<String> paths,
            HashMap<String, Object> opt) {
        boolean verbose = opt != null && opt.containsKey("verbose") && (boolean) opt.get("verbose");
        Gson gson = null;
        if (verbose) {
            gson = new GsonBuilder().create();
        }
        ArrayList<Pattern> matches = null;
        if (opt != null && opt.containsKey("matches")) {
            matches = new ArrayList<Pattern>();
            for (String p : (ArrayList<String>) opt.get("matches")) {
                matches.add(Pattern.compile(p));
            }
        }
        ArrayList<Pattern> excludes = null;
        if (opt != null && opt.containsKey("excludes")) {
            excludes = new ArrayList<Pattern>();
            for (String p : (ArrayList<String>) opt.get("excludes")) {
                excludes.add(Pattern.compile(p));
            }
        }
        String root_dir_pattern = "^[a-zA-Z]:[/]*$|^[/]+$|^[/]+cygdrive[/]+[^/]+[/]*$";
        HashMap<String, HashMap<String, ArrayList>> AllowDenyPatterns = new HashMap<String, HashMap<String, ArrayList>>();
        if (opt.containsKey("AllowDenyPatterns")) {
            AllowDenyPatterns = (HashMap<String, HashMap<String, ArrayList>>) opt.get("AllowDenyPatterns");
        }
        // note: Java doesn't have chdir() or pwd concept as it is not multi-threading
        // safe
        HashMap<String, HashMap> tree = new HashMap<>();
        HashMap<String, HashMap> other = new HashMap<>();
        for (String path : paths) {
            ArrayList<String> globs = null;
            try {
                globs = FileGlob.get(path, null);
            } catch (IOException e1) {
                e1.printStackTrace();
                continue;
            }
            System.out.println("resolved globs if any: " + path + " => " + gson.toJson(globs));
            for (String p : globs) {
                File f = new File(p);
                if (!f.exists()) {
                    System.err.println(p + " not found");
                    continue;
                }
                String abs_path = f.getAbsolutePath().toString();
                abs_path = abs_path.replace("\\", "/");
                if (abs_path.matches(root_dir_pattern)) {
                    System.err.println("we cannot handle root dir: " + p + " = " + abs_path);
                    System.exit(1); // exit here, not just skip, as mishandle this could remove files.
                }
                // find front and back
                String front = null;
                String back = null;
                Pattern pattern = Pattern.compile("^(.*/)(.+)");
                Matcher matcher = pattern.matcher(abs_path);
                if (matcher.find()) {
                    front = matcher.group(1);
                    back = matcher.group(2);
                } else {
                    System.err.println("unexpected path format, no '/' : " + abs_path);
                    System.exit(1); // exit here, not just skip, as mishandle this could remove files.
                }
                // https://stackoverflow.com/questions/41117898/how-to-create-empty-enumset
                EnumSet<FileVisitOption> opts = EnumSet.noneOf(FileVisitOption.class);
                FileWalker walker = new FileWalker(front, back);
                try {
                    walker.walk(abs_path, back, 30);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tree.putAll(walker.tree);
            }
        }
        return tree;
    }
    // <TREE>
    // key=junkdir|back=junkdir|front=/home/axptsusu/1mode=07551mtime=15452373341size=1281
    // test=dir|type=dir
    // key=junkdir/junk.tar|back=junkdir|front=/home/axptsusu/|mode=0644|mtime=1545237337|size=4165632|type=file
    // // </TREE>

    public static void main(String[] args) {
        HashMap<String, Object> opt = new HashMap<String, Object>();
        opt.put("verbose", true);
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("C:/users/william/git*/tpsup/ps1");
        paths.add("C:/users/william/git*/kdb");
        paths.add("C:/users/william/github/tpsup/profile");
        HashMap<String, HashMap> tree = build_dir_tree(paths, opt);
        System.out.println((new GsonBuilder().setPrettyPrinting().create()).toJson(tree));
    }
}
