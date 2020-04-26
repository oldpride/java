package com.tpsup.tpdist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class ToPull {
	// this will be a static class: all methods are static
	// to allow static methods to access class variables, the variables must be
	// declared static
	public static String root_dir_pattern = "^[a-zA-Z]:[/]*$|^[/]+$|^[/]+cygdrive[/]+[^/]+[/]*$";

	public static void pull(MyConn myconn, ArrayList<String> remote_paths, String local_dir,
			HashMap<String, Object> opt) {
		boolean dryrun = (Boolean) opt.getOrDefault("dryrun", false);
		boolean diff = (Boolean) opt.getOrDefault("diff", false);

		// replace \ with /, remove ending /
		local_dir.replace("\\", "/").replaceAll("/+$", "");

		// String local_dir_abs = (new File(local_dir)).getAbsolutePath();
		ArrayList<String> local_paths = new ArrayList<String>();
		for (String remote_path : remote_paths) {
			// replace \ with /, remove ending /
			remote_path.replace("\\", "/").replaceAll("/+$", "");
			if (remote_path.matches(ToPull.root_dir_pattern)) {
				String message = "ERROR: cannot copy from root dir: " + remote_path;
				myconn.writeLine(message);
				MyLog.append(message);
				return;
			}
			// get the last component; we will treat it as a subdir right under local_dir
			String back;
			{
				String[] array = remote_path.split("/+");
				back = array[array.length - 1];
			}
			String local_path = local_dir + "/" + back;
			// resolve dir/*csv to dir/a.csv, a/b.csv, ...
			// example:
			// $0 client host port /a/b/c/*.csv d
			// we need to check whether we have d/*.csv
			ArrayList<String> globs = FileGlob.get(local_path, null);
			if (globs.isEmpty()) {
				continue;
			}
			for (String path : globs) {
				String local_abs = (new File(path)).getAbsolutePath().toString().replace("\\", "/");
				local_paths.add(local_abs);
			}
		}
		
		// excludes and matches are coming from two places
		// 1. from command line. a single string, multiple patterns are separated by
		// comma, ",".
		// 2. from remote pull request, a single string, multiple patterns are separated
		// by newline, "\n"
		// we will need this when build_dir_tree
		String matches_str = (String) opt.getOrDefault("matches", "");
		String excludes_str = (String) opt.getOrDefault("excludes", "");
		opt.put("matchExclude", new MatchExclude(matches_str, excludes_str, "[,\n]"));

		MyLog.append("building local tree using abs_path: " + local_paths.toString());
		int maxsize = (Integer) opt.getOrDefault("maxsize", -1);
		HashMap<String, HashMap<String, String>> local_tree = DirTree.build_dir_tree(local_paths, opt);
		MyLog.append("local_tree = " + local_tree);

		// myconn.configureBlocking(true); we don't need to this because we
		// use OutputStream to write, which is always blocking
		String version_string = "<VERSION>" + Env.version + "</VERSION>";
		MyLog.append("sending version: " + version_string);
		myconn.writeLine(version_string);

		String uname_string = "<UNAME>Java|" + Env.uname + "</UNAME>";
		MyLog.append("sending uname: " + uname_string);
		myconn.writeLine(uname_string);

		String paths_string = "<PATH>" + String.join("|", remote_paths) + "</PATH>";
		MyLog.append("sending path: " + paths_string);
		myconn.writeLine(paths_string);

		String deep = (String) opt.getOrDefault("Deep", "0");
		String deep_string = "<DEEP>" + deep + "</DEEP>";
		MyLog.append("sending deep check flag: " + deep_string);
		myconn.writeLine(deep_string);

		int skipped = 0;
		String local_tree_string = null;
		{
			StringBuilder bld = new StringBuilder();
			bld.append("<TREE>\n");
			for (String f : local_tree.keySet()) {
				HashMap<String, String> node = local_tree.get(f);
				if (node.containsKey("skip")) {
					// don't send skipped file to remote; this way, remote will not tell us to
					// delete them.
					skipped++;
					continue;
				}

				ArrayList<String> list = new ArrayList<String>();
				list.add("key=" + f);
				for (String attr : node.keySet()) {
					String string = attr + "=" + node.get(attr);
					list.add(string);
				}
				String line = StringUtils.join(list, "|");
				bld.append(line);
				bld.append("\n");
			}
			bld.append("</TREE>");
			local_tree_string = bld.toString();
		}
		MyLog.append("sending local_tree: " + local_tree.size() + " items. skipped " + skipped);
		MyLog.append(MyLog.VERBOSE, local_tree_string);
		myconn.writeLine(local_tree_string);

		String maxsize_string = "<MAXSIZE>" + maxsize + "</MAXSIZE>";
		MyLog.append("sending maxsize: " + maxsize_string);
		myconn.writeLine(maxsize_string);

		// excludes and matches are coming from command line. a single string, multiple
		// patterns are separated by comma, ",".
		// we need to replace the comma with newline
		String excludes_string = "<EXCLUDE>" + ((String) opt.getOrDefault("excludes", "")).replace(",", "\n")
				+ "</EXCLUDE>";
		MyLog.append("sending excludes_string: " + excludes_string);
		myconn.writeLine(excludes_string);

		String matches_string = "<MATCH>" + ((String) opt.getOrDefault("matches", "")).replace(",", "\n") + "</MATCH>";
		MyLog.append("sending matches_string: " + matches_string);
		myconn.writeLine(matches_string);
		myconn.flush();

		MyLog.append("waiting cksum requests from server ...");

		ExpectSocket expectSocket = new ExpectSocket(myconn, opt);

		String[] patternArray = { "<NEED_CKSUMS>(.*)</NEED_CKSUMS>" };
		ArrayList<ArrayList<String>> captures = expectSocket.capture(patternArray, opt);
		if (captures == null) {
			return;
		}
		String need_cksums_string = captures.get(0).get(0);

		String[] need_cksums = need_cksums_string.split("\n");
		MyLog.append("received cksum requests, " + need_cksums.length + " items");
		HashMap<String, String> local_cksums_by_file = Cksum.get_cksums(need_cksums, local_tree);

		String cksums_results_string = null;
		{
			StringBuilder bld = new StringBuilder();
			bld.append("<CKSUM_RESULTS>\n");
			for (String file : local_cksums_by_file.keySet()) {
				bld.append(local_cksums_by_file.get(file) + " " + file + "\n");
			}
			bld.append("</CKSUM_RESULTS>\n");
			cksums_results_string = bld.toString();
		}

		// myconn.configureBlocking(true); not need this as OutputStream is always
		// blocking
		MyLog.append("sending cksums results: " + local_cksums_by_file.size() + " items");
		MyLog.append(MyLog.VERBOSE, cksums_results_string);
		myconn.writeLine(cksums_results_string);
		myconn.flush();

		MyLog.append("waiting instructions from remote...");
		String[] patternArray2 = { "<DELETES>(.*)</DELETES>", "<MTIMES>(.*)</MTIMES>", "<MODES>(.*)</MODES>",
				"<SPACE>(\\d+)</SPACE>", "<ADDS>(.*)</ADDS>", "<WARNS>(.*)</WARNS>" };

		captures.clear();
		captures = expectSocket.capture(patternArray2, opt);

		if (captures == null) {
			return;
		}

		String deletes_string = captures.get(0).get(0);
		String mtimes_string = captures.get(1).get(0);
		String modes_string = captures.get(2).get(0);
		String RequiredSpace_string = captures.get(3).get(0);
		String adds_string = captures.get(4).get(0);
		String warns_string = captures.get(5).get(0);

		if (!deletes_string.isEmpty()) {
			String[] deletes = deletes_string.split("\n");
			String last_delete = "";
			for (String d : deletes) {
				d = d.replace("\\", "/");
				if (last_delete.isEmpty() || !d.matches("^" + last_delete + "/.*")) {
					// if we already deleted the dir, no need to delete files under it.
					MyLog.append("rm -fr " + d);
					String front = (String) local_tree.get(d).get("front");
					try {
						String localPath = front + "/" + d;
						File f = new File(localPath);
						if (!f.exists()) {
							MyLog.append(localPath + " does't exist locally");
						} else {
							if (!dryrun && !diff) {
								if (f.isDirectory()) {
									FileUtils.deleteDirectory(f);
								} else {
									FileUtils.forceDelete(f);
								}
							}
						}
					} catch (IOException e) {
						MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
					}
					last_delete = d;
				}
			}
		}

		ArrayList<String> diff_files = new ArrayList<String>();

		if (!adds_string.isEmpty()) {
			HashMap<String, String> action_by_file = new HashMap<String, String>();
			String[] adds = adds_string.split("\n");
			Pattern pair_pattern = Pattern.compile("^[ ]*([^ ]+)[ ](.+)");
			for (String a : adds) {
				a = a.replace("\\", "/");
				String action;
				String file;
				Matcher pair_matcher = pair_pattern.matcher(a);
				if (pair_matcher.find()) {
					action = pair_matcher.group(1);
					file = pair_matcher.group(2);

					if (action_by_file.containsKey(file)) {
						MyLog.append(MyLog.ERROR, "file appeared more than once on remote side");
					}
					action_by_file.put(file, action);

					if (action == "update") {
						diff_files.add(file);
					}
				} else {
					String error_message = "unexpected format " + a + ". expecting: action file";
					MyLog.append(MyLog.ERROR, error_message);
					myconn.writeLine(error_message);
					myconn.flush();
					return;
				}
			}
			for (String f : action_by_file.keySet()) {
				MyLog.append(String.format("%10s %s", action_by_file.get(f), f));
			}
		}

		if (!warns_string.isEmpty()) {
			String[] warns = warns_string.split("\n");
			for (String w : warns) {
				MyLog.append("warning from server side: " + w);
			}
		}

		opt.put("chkSpace", RequiredSpace_string);
		String tmpBase = (String) opt.getOrDefault("tmpdir", Env.tmpBase);
		String tmp_tar_file = TmpFile.createTmpFile(tmpBase, "tpdist", opt);

		if (tmp_tar_file == null) {
			MyLog.append(MyLog.ERROR, "failed to create tmpFile");
			return;
		}
		MyLog.append("tmpFile = " + tmp_tar_file);

		if (adds_string.isEmpty()) {
			MyLog.append("nothing to add or update\n");
			// don't return here as we will other work to do
		} else {
			// myconn.configureBlocking(true); not need this as OutputStream is always
			// blocking
			if (diff) {
				myconn.writeLine("please send diff");
			} else {
				myconn.writeLine("please send data");
			}
			myconn.flush();

			String tmp_diff_dir = null;
			if (diff) {
				tmp_diff_dir = TmpFile.createTmpFile(tmpBase, "tpdist_dir", opt);
				if (tmp_diff_dir == null) {
					MyLog.append(MyLog.ERROR, "failed to create " + tmp_diff_dir);
				}
			}

			MyLog.append("waiting for data from remote, will write to $tmp_tar_file");

			int tar_size = 0;
			try {
				OutputStream outStream = new FileOutputStream(tmp_tar_file);

				byte buffer[] = new byte[1024 * 1024];
				int size;
				while ((size = myconn.streamReadBytes(buffer)) != -1) {
					// Stream is blocking IO but we don't worry here as it is the last network
					// communication. As it is blocked io, no need to use sleep to throttle.
					outStream.write(buffer, 0, size);
					tar_size += size;
				}
				outStream.close();
				MyLog.append("received tar_size=" + tar_size + "\n");
			} catch (IOException e) {
				MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
			}

			if (tar_size == 0) {
				MyLog.append("no new file to add");
			} else {
				try {
					Tar.unTar(tmp_tar_file, local_dir);
				} catch (Exception e) {
					MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
					MyLog.append(MyLog.ERROR, "unTar(" + tmp_tar_file + ", " + local_dir + ") failed");
					return;
				}
				MyLog.append("extracted tar files from " + tmp_tar_file + " to " + local_dir);
			}

			if ((Boolean) opt.getOrDefault("KeepTmpFile", false)) {
				MyLog.append("tmp file " + tmp_tar_file + " is kept");
			} else {
				MyLog.append("removing tmp file " + tmp_tar_file);
				try {
					FileUtils.forceDelete(new File(tmp_tar_file));
				} catch (IOException e) {
					MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
				}
			}

			if (diff) {
				for (String f : diff_files) {
					String local_f = local_tree.get(f).get("front") + f;
					String tmp_f = tmp_diff_dir + f;
					MyLog.append("diff " + local_f + " " + tmp_f);
					Diff.diff(local_f, tmp_f);
				}

				if ((Boolean) opt.getOrDefault("KeepTmpFile", false)) {
					MyLog.append("tmp_diff_dir " + tmp_diff_dir + " is kept");
				} else {
					MyLog.append("removing tmp_diff_dir " + tmp_diff_dir);
					try {
						FileUtils.deleteDirectory(new File(tmp_diff_dir));
					} catch (IOException e) {
						MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
					}
				}
			}
		}

		if (!mtimes_string.isEmpty()) {
			Pattern pattern = Pattern.compile("^(\\d+?)[ ](.+)$");
			for (String l : mtimes_string.split("\n")) {
				if (l.isEmpty()) {
					continue;
				}
				Matcher matcher = pattern.matcher(l);
				if (matcher.find()) {
					String mtime = matcher.group(1);
					String filename = matcher.group(2);
					FileTime fileTime = FileTime.fromMillis((long) (Integer.parseInt(mtime)) * 1000);
					String fullName = local_dir + "/" + filename;
					BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(fullName),
							BasicFileAttributeView.class);
					MyLog.append("set mtime: " + mtime + " " + filename);
					try {
						attributes.setTimes(fileTime, fileTime, fileTime);
					} catch (IOException e) {
						MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
					}
				} else {
					MyLog.append(MyLog.ERROR, "bad mtime-file format at line : '" + l + "'");
				}
			}
		}

		if (!modes_string.isEmpty()) {
			Pattern pattern = Pattern.compile("^(\\S+?)\\s+(\\S.*)$");
			for (String l : modes_string.split("\n")) {
				if (l.isEmpty()) {
					continue;
				}
				Matcher matcher = pattern.matcher(l);
				if (matcher.find()) {
					String mode = matcher.group(1);
					String filename = matcher.group(2);
					String fullname = local_dir + "/" + filename;
					MyLog.append("don't know how to set mode: " + mode + " " + fullname);
				} else {
					MyLog.append(MyLog.ERROR, "bad mode-file format at line : '" + l + "'");
				}
			}
		}
		MyLog.append("all done\n\n");
	}
}
