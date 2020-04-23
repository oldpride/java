package com.tpsup.tpdist;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirTree {
	public static HashMap<String, HashMap> build_dir_tree(ArrayList<String> paths, HashMap<String, Object> opt) {	
		String root_dir_pattern = "^[a-zA-Z]:[/]*$|^[/]+$|^[/]+cygdrive[/]+[^/]+[/]*$";
		HashMap<String, HashMap<String, ArrayList>> AllowDenyPatterns = new HashMap<String, HashMap<String, ArrayList>>();
		if (opt.containsKey("AllowDenyPatterns")) {
			AllowDenyPatterns = (HashMap<String, HashMap<String, ArrayList>>) opt.get("AllowDenyPatterns");
		}
		// note: Java doesn't have chdir() or pwd concept as it is not multi-threading
		// safe
		HashMap<String, HashMap> tree = new HashMap<String, HashMap>();
		HashMap<String, HashMap> other = new HashMap<String, HashMap>();
		for (String path : paths) {
			ArrayList<String> globs = null;
			globs = FileGlob.get(path, null);
			MyLog.append("resolved globs if any: " + path + " => " + MyGson.toJson(globs));
			for (String p : globs) {
				File f = new File(p);
				if (!f.exists()) {
					MyLog.append(p + " not found");
					continue;
				}
				String abs_path = f.getAbsolutePath().toString();
				abs_path = abs_path.replace("\\", "/");
				if (abs_path.matches(root_dir_pattern)) {
					MyLog.append(MyLog.ERROR, "we cannot handle root dir: " + p + " = " + abs_path);
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
					// System.err.println("unexpected path format, no '/' : " + abs_path);
					MyLog.append(MyLog.ERROR, "unexpected path format, no '/' : " + abs_path);
					System.exit(1); // exit here, not just skip, as mishandle this could remove files.
				}
				// https://stackoverflow.com/questions/41117898/how-to-create-empty-enumset
				EnumSet<FileVisitOption> opts = EnumSet.noneOf(FileVisitOption.class);
				FileWalker walker = new FileWalker(front, back, opt);
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
		MyLog.append(MyGson.toJson(tree));
	}
}
