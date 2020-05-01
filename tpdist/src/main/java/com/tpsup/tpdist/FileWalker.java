package com.tpsup.tpdist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

public class FileWalker {
	public HashMap<String, HashMap<String, String>> tree;
	public String front;
	public String back;
	private MatchExclude matchExclude;
	private Access access;

	public FileWalker(String front, String back, HashMap<String, Object> opt) {
		this.tree = new HashMap<String, HashMap<String, String>>();
		this.front = front;
		this.back = back;
		this.access = (Access) opt.get("access");
        this.matchExclude = (MatchExclude) opt.get("matchExclude");
	}

	// "path" is the full path,
	// "relative" is the tail part, starting after $back
	// remote and local cannot compare absolute paths as they are likely different
	// directory structure. however, the tail part should be the same. therefore, we
	// use the tail part as key.
	public void walk(String path, String relative, int level) throws IOException {
		File f = new File(path);
		HashMap<String, String> kv = new HashMap<String, String>();
		if (!this.matchExclude.pass(path)) {
			MyLog.append(path + " didn't pass match/exclude patterns");
			// don't return here as the child files or dirs may still match the pattern. just don't add it to the return tree
			//return;
		} else {
			this.tree.put(relative, kv);
		}

		if (!this.access.is_allowed("file", path)) {
			MyLog.append(path + " not allowed");
			kv.put("skip", "not allowed");
			return;
		}

		if (Files.isSymbolicLink(f.toPath())) {
			kv.put("type", "link");
			kv.put("mode", "0777");
			kv.put("size", "22"); // hardcoded for sym link
			kv.put("test", f.toPath().toRealPath().toString());
			kv.put("mtime", String.valueOf(f.lastModified() / 1000));
			kv.put("front", this.front);
			kv.put("back", this.back);
			MyLog.append(MyLog.VERBOSE, String.format("Symbolic link: %s", f));
		} else if (Files.isRegularFile(f.toPath())) {
			kv.put("type", "file");
			kv.put("mode", "0644");
			kv.put("size", String.valueOf(Files.size(f.toPath())));
			kv.put("mtime", String.valueOf(f.lastModified() / 1000));
			kv.put("front", this.front);
			kv.put("back", this.back);
			MyLog.append(MyLog.VERBOSE, String.format("Regular file: %s", f));
		} else if (f.isDirectory()) {
			level--;
			if (level < 0) {
				String message = "Dir: " + relative + " too deep to parse";
				kv.put("skip", message);
				MyLog.append(MyLog.ERROR, message);
				return;
			}
			kv.put("type", "dir");
			kv.put("mode", "0755");
			kv.put("size", "128"); // hard coded for directory
			kv.put("test", "dir");
			kv.put("mtime", String.valueOf(f.lastModified() / 1000));
			kv.put("front", this.front);
			kv.put("back", this.back);
			MyLog.append(MyLog.VERBOSE, "Dir: " + relative);

			String[] list = f.list();
			if (list == null)
				return;
			for (String shortname : list) {
				String new_path = path + "/" + shortname;
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
