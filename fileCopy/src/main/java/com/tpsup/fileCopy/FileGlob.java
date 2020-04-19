package com.tpsup.fileCopy;

// C:/a/b/*/d should return
//    C:/a/b/c1/d
//    C:/a/b/c2/d
// C:/a/b/*/nosuchfile should return C:/a/b/*/nosuchfile
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileGlob {
	public static ArrayList<String> get(String pattern, HashMap<String, Object> opt) {
		ArrayList<String> results = new ArrayList<String>();
		boolean verbose = opt != null && (Boolean) opt.getOrDefault("verbose", false);
		// default to check file existence
		boolean checkExistence = opt == null || (Boolean) opt.getOrDefault("checkExistence", true);
		// return the string if it is not pattern.
		{
			if (!pattern.contains("*") && !pattern.contains("?")) {
				if (!checkExistence || (new File(pattern).exists())) {
					results.add(pattern);
					return results;
				}
			}
		}
		// convert \ to /. remove ending /
		pattern.replace("\\", "/").replace("/+$", "");
		// we need to start from the part without * or ?
		ArrayList<String> parts = new ArrayList<String>(Arrays.asList(pattern.split("/")));
		if (parts.get(0).contains("*") || parts.get(0).contains("?")) {
			// if the path is started with a glob char, eg, *, git*, change it to ./*,
			// ./git*
			parts.add(0, ".");
		}
		int full_length = parts.size();
		boolean[] is_glob = new boolean[full_length];
		PathMatcher[] compiled_patterns = new PathMatcher[full_length];
		for (int i = 0; i < full_length; i++) {
			String part = parts.get(i);
			if (part.isEmpty()) {
				is_glob[i] = false;
				continue;
			}
			is_glob[i] = (part.contains("*") || part.contains("?"));
			if (is_glob[i]) {
				compiled_patterns[i] = FileSystems.getDefault().getPathMatcher("glob:" + part);
			}
		}
		
		MyLogger.append(MyLogger.VERBOSE, "is_glob = " + MyGson.gson.toJson(is_glob));

		ArrayList<ArrayList<String>> todo = new ArrayList<ArrayList<String>>();
		ArrayList<String> seed = new ArrayList<String>();
		seed.add(parts.get(0));
		todo.add(seed);
		while (true) {
			if (todo.isEmpty()) {
				break;
			}

			MyLogger.append(MyLogger.VERBOSE, "todo = " + MyGson.gson.toJson(todo));

			ArrayList<String> doing = todo.remove(todo.size() - 1); // pop out the last todo item
			int i = doing.size();
			String next_part = parts.get(i);
			if (is_glob[i]) {
				// there is glob char in this pattern part
				String current_path = String.join("/", doing);
				File[] list = (new File(current_path)).listFiles();
				if (list != null) {
					for (File f : list) {
						// System.out.println(f.getPath()); // fullname
						// System.out.println(f.getName()); // shortname
						String shortname = f.getName();
						Path shortPath = Paths.get(shortname);
						if (verbose) {
							System.out.println(shortname + " vs " + parts.get(i));
						}
						if (compiled_patterns[i].matches(shortPath)) {
							ArrayList<String> new_item = (ArrayList<String>) doing.clone();
							new_item.add(shortname);
							if (new_item.size() == full_length) {
								results.add(String.join("/", new_item));
							} else {
								todo.add(new_item);
							}
						}
					}
				}
			} else {
				// no glob char in this pattern part
				doing.add(next_part);
				String path = String.join("/", doing);
				if ((new File(path)).exists()) {
					if (doing.size() == full_length) {
						results.add(path);
					} else {
						todo.add(doing);
					}
				} // else, this branch is discarded
			}
		}
		if (results.isEmpty() && !checkExistence) {
			results.add(pattern);
		}
		return results;
	}

	public static void test() {
		File[] list = (new File("C:/users/william/github/tpsup/ps1")).listFiles();
		if (list != null) {
			for (File f : list) {
				System.out.println("fullname = " + f.getPath()); // fullname
				System.out.println("shortname = " + f.getName()); // shortname
				String shortname = f.getName();
				System.out.println("shortname to Path = " + Paths.get(shortname));
				System.out.println("file to Path = " + f.getPath());
			}
		}
	}

	static void usage() {
		System.err.println("java FileGlob \"<glob_pattern>\"");
		System.err.println("java FileGlob \"C:/users/william/git*/tpsup/*/*ps1\"");
		System.exit(-1);
	}

	public static void main(String[] args) {
		if (args.length != 1)
			usage();
		String pattern = args[0];
		// System.out.println((new FileGlob()).get(pattern, null).toString());
		// test();
		HashMap<String, Object> opt = new HashMap<String, Object>();
		opt.put("verbose", true);
		opt.put("checkExistence", true);
		System.out.println(pattern + "=" + FileGlob.get(pattern, opt).toString());
		// System.out.println("C:/users/william/*/github/ps1" + "="
		// + FileGlob.get("C:/users/william/*/github/ps1", opt).toString());
		// System.out.println("*" + "=" + FileGlob.get("*", opt).toString());
	}
}
