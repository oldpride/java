package com.tpsup.tpdist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class FileWalker {
	public HashMap<String, HashMap> tree;
	public String front;
	public String back;
	private MatchExclude matchExclude;

	public FileWalker(String front, String back, HashMap<String, Object> opt) {
		this.tree = new HashMap<String, HashMap>();
		this.front = front;
		this.back = back;

		// excludes and matches are coming from two places
		// 1. from command line. a single string, multiple patterns are separated by
		// comma, ",".
		// 2. from remote pull request, a single string, multiple patterns are separated
		// by
		// newline, "\n"
		String matches_string = (String) opt.getOrDefault("matches", "");
		String excludes_string = (String) opt.getOrDefault("excludes", "");
		this.matchExclude = new MatchExclude(matches_string, excludes_string, "[,\n]");
	}

	public void walk(String path, String relative, int level) throws IOException {
		File f = new File(path);
		if (!this.matchExclude.pass(path)) {
			return;
		}
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
				MyLog.append(MyLog.ERROR, "Dir: " + relative + " too deep to parse");
				return;
			}
			String[] list = f.list();
			if (list == null)
				return;
			for (String shortname : list) {
				String new_path = path + "/" + shortname;
				if (!this.matchExclude.pass(new_path)) {
					continue;
				}
				walk(new_path, relative + "/" + shortname, level);
			}
		} else {
			MyLog.append(MyLog.VERBOSE, String.format("Other: %s \n", f));
		}
	}

	public static void main(String[] args) {
		MyLog.append("test split = " + MyGson.toJson("hello,world\nhi\ngood\n".split("[,\n]")));
	}
}
