package com.tpsup.tpdist;

// C:/a/b/*/d should return
//    C:/a/b/c1/d
//    C:/a/b/c2/d
// C:/a/b/*/nosuchfile should return C:/a/b/*/nosuchfile
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileGlob {
	public static ArrayList<String> get(String pattern, HashMap<String, Object> opt) {
		ArrayList<String> results = new ArrayList<String>();
		if (opt == null) {
			opt = new HashMap<String, Object>();
		}
		// default to check file existence
		boolean checkExistence = (Boolean) opt.getOrDefault("checkExistence", true);
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
		pattern.replace("\\", "/").replaceAll("/+$", "");
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

		MyLog.append(MyLog.VERBOSE, "is_glob = " + MyGson.toJson(is_glob));

		ArrayList<ArrayList<String>> todo = new ArrayList<ArrayList<String>>();
		ArrayList<String> seed = new ArrayList<String>();
		seed.add(parts.get(0));
		todo.add(seed);
		while (true) {
			if (todo.isEmpty()) {
				break;
			}

			MyLog.append(MyLog.VERBOSE, "todo = " + MyGson.gson.toJson(todo));

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
						MyLog.append(MyLog.VERBOSE, shortname + " vs " + parts.get(i));
						if (compiled_patterns[i].matches(shortPath)) {
							// https://stackoverflow.com/questions/9252803/how-to-avoid-unchecked-cast-warning-when-cloning-a-hashset
							// both the following commands will clone a new_item from "doing", the first one
							// is a little bit faster but will cause a warning: unchecked cast.
							// ArrayList<String> new_item = (ArrayList<String>) doing.clone();
							ArrayList<String> new_item = new ArrayList<String>(doing);
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
		MyLog.append("test cloning arrayList by creatation");
		ArrayList<Integer> a = new ArrayList<Integer>();
		a.add(1);
		a.add(2);
		ArrayList<Integer> b = new ArrayList<Integer>(a);
		b.add(3);
		MyLog.append("a = " + MyGson.toJson(a));
		MyLog.append("b = " + MyGson.toJson(b));

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
