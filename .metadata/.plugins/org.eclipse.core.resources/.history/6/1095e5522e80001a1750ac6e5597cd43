package com.tpsup.fileCopy;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirTree {
    public static HashMap<String, HashMap> build_dir_tree(ArrayList<String> startingDirList,
            String matchString, String excludeString) {
        HashMap<String, HashMap> tree = new HashMap<>();
        for (String startingDir : startingDirList) {
            tree.putAll(build_dir_tree_from_single_dir(startingDir, matchString, excludeString));
        }
        return tree;
    }

    public static HashMap<String, HashMap> build_dir_tree(String startingDirs, String matchString,
            String excludeString) {
        HashMap<String, HashMap> tree = new HashMap<>();
        for (String startingDir : startingDirs.split("\\|")) {
            tree.putAll(build_dir_tree_from_single_dir(startingDir, matchString, excludeString));
        }
        return tree;
    }

    public static HashMap<String, HashMap> build_dir_tree_from_single_dir(String startingDir,
            String matchString, String excludeString) {
        HashMap<String, HashMap> tree = new HashMap<>();
        Pattern matchPattern = Pattern.compile(matchString);
        Pattern excludePattern = Pattern.compile(excludeString);
        Path startingPath = Paths.get(startingDir);
        if (!startingPath.toFile().exists()) {
            System.out.println(startingDir + " not found");
            return tree;
        }
        String abs = startingPath.toFile().getAbsolutePath().toString();
        // https://stackoverflow.com/questions/680502
        // 8/how-to-replace-backward-slash-to-forward-slash-using-java
        abs = abs.replace("\\", "/");
        System.out.println(startingDir + "abs path is " + abs);
        if (!matchString.equals("")) {
            Matcher matchMatcher = matchPattern.matcher(abs);
            if (!matchMatcher.find()) {
                return tree;
            }
        }
        if (!excludeString.equals("")) {
            Matcher excludeMatcher = excludePattern.matcher(abs);
            if (excludeMatcher.find()) {
                return tree;
            }
        }
        Pattern pattern = Pattern.compile("^(.*/)(.+)");
        Matcher matcher = pattern.matcher(abs);
        if (matcher.find()) {
            String front = matcher.group(1);
            String back = matcher.group(2);
            // https://stackoverflow.com/questions/41117898/how-to-create-empty-enumset
            EnumSet<FileVisitOption> opts = EnumSet.noneOf(FileVisitOption.class);
            FileWalker walker = new FileWalker(front, back);
            try {
                walker.walk(startingDir, back, 30);
            } catch (IOException e) {
                e.printStackTrace();
            }
            tree.putAll(walker.tree);
        } else {
            System.out.println(startingDir + "abs path is " + abs + " is not supported");
        }
        return tree;
    }
    // <TREE>
    // key=junkdir|back=junkdir|front=/home/axptsusu/1mode=07551mtime=15452373341size=1281
    // test=dir|type=dir
    // key=junkdir/junk.tar|back=junkdir|front=/home/axptsusu/|mode=0644|mtime=1545237337|size=4165632|type=file
    // // </TREE>
}
