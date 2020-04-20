package com.tpsup.fileCopy;

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

	public static void to_pull(MyConn myconn, String[] remote_paths, String local_dir, HashMap<String, Object> opt) {
		if (opt == null) {
			opt = new HashMap<String, Object>();
		}
		boolean dryrun = (Boolean) opt.getOrDefault("dryrun", false);
		boolean diff = (Boolean) opt.getOrDefault("diff", false);

		// replace \ with /, remove ending /
		local_dir.replaceAll("\\", "/").replaceAll("/+$", "");

		String local_dir_abs = (new File(local_dir)).getAbsolutePath();
		ArrayList<String> local_paths = new ArrayList<String>();
		for (String remote_path : remote_paths) {
			// replace \ with /, remove ending /
			remote_path.replaceAll("\\", "/").replaceAll("/+$", "");
			if (remote_path.matches(ToPull.root_dir_pattern)) {
				String message = "ERROR: cannot copy from root dir: " + remote_path;
				myconn.writeString(message);
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
				String local_abs = (new File(path)).getAbsolutePath().toString();
				local_paths.add(local_abs);
			}
		}
		MyLog.append("building local tree using abs_path: " + local_paths.toString());
		long maxsize = (Long) opt.getOrDefault("maxsize", -1);
		HashMap<String, HashMap> local_tree = DirTree.build_dir_tree(local_paths, opt);
		MyLog.append("local_tree = " + local_tree);

		// myconn.configureBlocking(true); we don't need to this because we
		// use OutputStream to write, which is always blocking
		String version_string = "<VERSION>" + Version.version + "</VERSION>\n";
		MyLog.append("sending version: " + Version.version);

		String uname_string = "<UNAME>Java|" + System.getProperty("os.name") + "</UNAME>\n";
		MyLog.append("sending uname: Java|" + System.getProperty("os.name"));
		myconn.writeString(uname_string);

		String paths_string = "<PATH>" + String.join("|", remote_paths) + "</PATH>\n";
		MyLog.append("sending path: " + paths_string);
		myconn.writeString(paths_string);

		char deep = (Character) opt.getOrDefault("Deep", "0");
		String deep_string = "<DEEP>" + deep + "</DEEP>\n";
		MyLog.append("sending deep check flag: " + 0);
		myconn.writeString(deep_string);

		String local_tree_string = null;
		{
			StringBuilder bld = new StringBuilder();
			bld.append("<TREE>\n");
			for (String f : local_tree.keySet()) {
				ArrayList<String> list = new ArrayList<String>();
				list.add("key=" + f);
				for (String attr : ((HashMap<String, String>) local_tree.get(f)).keySet()) {
					String string = attr + "=" + local_tree.get(f).get(attr);
					list.add(string);
				}
				String line = StringUtils.join(list, "|");
				bld.append(line);
				bld.append("\n");
			}
			bld.append("</TREE>\n");
			local_tree_string = bld.toString();
		}
		MyLog.append("sending local_tree: " + local_tree.size() + " items");
		myconn.writeString(local_tree_string);

		String maxsize_string = "<MAXSIZE>" + maxsize + "</MAXSIZE>\n";
		MyLog.append("sending maxsize: " + maxsize);
		myconn.writeString(maxsize_string);

		String excludes_string = "<EXCLUDE>" + (String) opt.getOrDefault("Excludes", "") + "</EXCLUDE>\n";
		MyLog.append("sending excludes_string: " + excludes_string);
		myconn.writeString(excludes_string);

		String matches_string = "<MATCH>" + (String) opt.getOrDefault("Matches", "") + "</MATCH>\n";
		MyLog.append("sending matches_string: " + matches_string);
		myconn.writeString(matches_string);
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
		myconn.writeString(cksums_results_string);
		myconn.flush();

		MyLog.append("waiting instructions from remote...");
		String[] patternArray2 = { "<DELETES>(.*)</DELETES>", "<MTIMES>(.*)</MTIMES>", "<MODES>(.*)</MODES>",
				"<SPACE>(\\d+)</SPACE>", "<ADDS>(.*)</ADDS>", "<WARNS>(.*)</WARNS>" };

		captures.clear();
		captures = expectSocket.capture(patternArray, opt);
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
					action_by_file.put(file, action);
				} else {
					String error_message = "unexpected format " + a + ". expecting: action file";
					MyLog.append(MyLog.ERROR, error_message);
					myconn.writeString(error_message);
					myconn.flush();
					return;
				}
			}
			for (String f : action_by_file.keySet()) {
				MyLog.append(String.format("%10s %s\n", action_by_file.get(f), f));
			}
		}

		if (!warns_string.isEmpty()) {
			String[] warns = warns_string.split("\n");
			for (String w : warns) {
				MyLog.append("warning from server side: " + w);
			}
		}

		opt.put("chkSpace", RequiredSpace_string);
		String tmpBase = System.getProperty("java.io.tmpdir");
		String tmp_tar_file = TmpFile.createTmpFile(tmpBase, "filecopy", opt);

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
				myconn.writeString("please send diff\n");
			} else {
				myconn.writeString("please send data\n");
			}
			myconn.flush();

			String tmp_diff_dir = null;
			if (diff) {
				tmp_diff_dir = TmpFile.createTmpFile(tmpBase, "filecopy_dir", opt);
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
			
			if ((Boolean)opt.getOrDefault("KeepTmpFile", false)) {
				MyLog.append("tmp file " + tmp_tar_file + " is kept");
			} else {
				MyLog.append("removing tmp file " + tmp_tar_file);
				try {
					FileUtils.forceDelete(new File(tmp_tar_file));
				} catch (IOException e) {
					MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
				}				
			}
		}
		
		
		if (!mtimes_string.isEmpty()) {
			Pattern pattern = Pattern.compile("^(\\d+?)[ ](.+)$");
			for (String line : mtimes_string.split("\n")) {
				if (line.isEmpty()) {
					continue;
				}
				Matcher matcher = pattern.matcher(line);
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
					MyLog.append(MyLog.ERROR, "bad format at line : '" + line + "'");
				}
			}
		}
		MyLog.append("all done\n\n");
	}


}
