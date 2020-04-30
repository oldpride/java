package com.tpsup.tpdist;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class DirTree {
	public static HashMap<String, HashMap<String, String>> build_dir_tree(ArrayList<String> paths,
			HashMap<String, Object> opt) {
		String root_dir_pattern = "^[a-zA-Z]:[/]*$|^[/]+$|^[/]+cygdrive[/]+[^/]+[/]*$";
		Access access = (Access) opt.get("access");

		// note: Java doesn't have chdir() or pwd concept as it is not multi-threading
		// safe

		String RelativeBase = (String) opt.getOrDefault("RelativeBase", null);
		Pattern abs_pattern = Pattern.compile("^/|^[a-zA-Z]:");

		HashMap<String, HashMap<String, String>> tree = new HashMap<String, HashMap<String, String>>();
		for (String path : paths) {
			path = path.replace("\\", "/");
			if (RelativeBase != null && !abs_pattern.matcher(path).find()) {
				path = RelativeBase + "/" + path;
			}

			ArrayList<String> globs = null;
			globs = FileGlob.get(path, null);
			if (globs == null) {
				String message = "glob cannot resolve " + path;
				MyLog.append(MyLog.ERROR, message);
				HashMap<String, String> node = new HashMap<String, String>();
				node.put("skip", message);
				tree.put(path, node);
				continue;
			}
			MyLog.append("resolved globs if any: " + path + " => " + MyGson.toJson(globs));

			for (String p : globs) {
				File f = new File(p);
				if (!f.exists()) {
					String message = p + " not found";
					MyLog.append(MyLog.ERROR, message);
					HashMap<String, String> node = new HashMap<String, String>();
					node.put("skip", message);
					tree.put(p, node);
					continue;
				}

				String abs_path = f.getAbsolutePath().toString();
				abs_path = abs_path.replace("\\", "/");
				if (abs_path.matches(root_dir_pattern)) {
					String message = "we cannot handle root dir: " + p + " = " + abs_path;
					MyLog.append(MyLog.ERROR, message);
					HashMap<String, String> node = new HashMap<String, String>();
					node.put("skip", message);
					tree.put(p, node);
					continue;
				}

				// $back is the starting point to compare, a relative path
				// $front is the parent path (absolute path)
				// example:
				// $0 client host port /a/b/*.csv /c/d/e f/g
				// $back will be *.csv and e.
				// when comparing *.csv, server needs to 'cd /a/b'. client needs to 'cd f/g'.
				// when comparing e, server needs to 'cd /c/d'. client needs to 'cd f/g'.
				String front = null;
				String back = null;
				Pattern pattern = Pattern.compile("^(.*/)(.+)");
				Matcher matcher = pattern.matcher(abs_path);
				if (matcher.find()) {
					front = matcher.group(1);
					back = matcher.group(2);
				} else {
					String message = "unexpected path format, not in 'front/back' form: " + abs_path;
					MyLog.append(MyLog.ERROR, message);
					HashMap<String, String> node = new HashMap<String, String>();
					node.put("skip", message);
					tree.put(p, node);
					continue;
				}

				// now that we have clearly identified $back, we should use it as key rather
				// than
				// $abs_path or $p as key
				if (!access.is_allowed("file", abs_path)) {
					String message = "not allowed";
					MyLog.append(message);
					HashMap<String, String> node = new HashMap<String, String>();
					node.put("skip", message);
					tree.put(back, node);
					continue;
				}

				FileWalker walker = new FileWalker(front, back, opt);
				try {
					walker.walk(abs_path, back, 30);
				} catch (IOException e) {
					MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e));
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

	// overload to accept Array
	public static HashMap<String, HashMap<String, String>> build_dir_tree(String[] paths, HashMap<String, Object> opt) {
		return build_dir_tree(new ArrayList<String>(Arrays.asList(paths)), opt);
	}

	public static void main(String[] args) {
		HashMap<String, Object> opt = new HashMap<String, Object>();
		opt.put("verbose", true);
		ArrayList<String> paths = new ArrayList<String>();
		paths.add("C:/users/william/git*/tpsup/ps1");
		paths.add("C:/users/william/git*/kdb");
		paths.add("C:/users/william/github/tpsup/profile");
		HashMap<String, HashMap<String, String>> tree = build_dir_tree(paths, opt);
		MyLog.append(MyGson.toJson(tree));
	}
}
