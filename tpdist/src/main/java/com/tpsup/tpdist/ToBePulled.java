package com.tpsup.tpdist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ToBePulled {
	public static void bePulled(MyConn myconn, HashMap<String, Object> opt) {
		MyLog.append("waiting information from remote ...");

		ExpectSocket expectSocket = new ExpectSocket(myconn, opt);

		String[] patternArray = { "<PATH>(.+)</PATH>", "<TREE>(.*)</TREE>", "<MAXSIZE>([-]?\\d+)</MAXSIZE>",
				"<VERSION>(.+)</VERSION>", "<EXCLUDE>(.*)</EXCLUDE>", "<MATCH>(.*)</MATCH>", "<DEEP>(.)</DEEP>",
				"<UNAME>(.+)</UNAME>" };
		ArrayList<ArrayList<String>> captures = expectSocket.capture(patternArray, opt);
		if (captures == null) {
			return;
		}
		String local_paths_string = captures.get(0).get(0);
		String remote_tree_block = captures.get(1).get(0);
		int maxsize = new Integer(captures.get(2).get(0));
		String remote_version = captures.get(3).get(0);
		String exclude_string = captures.get(4).get(0);
		String match_string = captures.get(5).get(0);
		int deep_check = new Integer(captures.get(6).get(0));
		String uname = captures.get(7).get(0);

		String[] remote_version_split = remote_version.split("[.]");
		String peer_protocol = remote_version_split[0];

		if (!peer_protocol.equals(Env.expected_peer_protocol)) {
			MyLog.append(MyLog.ERROR, "remote used wrong protocol " + peer_protocol + ", we are expecting protocol "
					+ Env.expected_peer_protocol + ". we closed the connection.");
			myconn.writeLine("wrong protocol " + peer_protocol + ", we are expecting protocol "
					+ Env.expected_peer_protocol + ". we closed the connection.");
			myconn.flush();
			return;
		}

//		boolean check_mode = (Env.isUnix
//				&& (uname.toLowerCase().contains("unix") || uname.toLowerCase().contains("ux")))
//				|| (Env.isWindows && uname.toLowerCase().contains("windows"));
		// todo: I don't really know how to get windows or UNIX mode within java.
		boolean check_mode = false;
		MyLog.append("remote uname='" + uname + "'. we set check_mode=" + check_mode);

		HashMap<String, HashMap<String, String>> remote_tree = new HashMap<String, HashMap<String, String>>();
		if (!remote_tree_block.isEmpty()) {
			Pattern line_pattern = Pattern.compile("^key=");
			Pattern pair_pattern = Pattern.compile("^(.+?)=(.+)");
			for (String line : remote_tree_block.split("\n")) {
				if (line.isEmpty()) {
					continue;
				}
				Matcher line_matcher = line_pattern.matcher(line);
				if (!line_matcher.find()) {
					continue;
				}
				HashMap<String, String> kv = new HashMap<String, String>();
				for (String pair : line.split("[|]")) {
					Matcher pair_matcher = pair_pattern.matcher(pair);
					if (pair_matcher.find()) {
						kv.put(pair_matcher.group(1), pair_matcher.group(2));
					}
				}
				remote_tree.put(kv.get("key"), kv);
			}
		}
		MyLog.append(MyLog.VERBOSE, "remote_tree = " + MyGson.toJson(remote_tree));

		MyLog.append("building local_tree using paths: " + local_paths_string);

		String[] local_paths = local_paths_string.split("[|]");

		// excludes and matches are coming from two places
		// 1. from command line. a single string, multiple patterns are separated by
		// comma, ",".
		// 2. from remote pull request, a single string, multiple patterns are separated
		// by newline, "\n"
		// we need this when build_dir_tree
		opt.put("matchExclude", new MatchExclude(match_string, exclude_string, "[,\n]"));
		
		// on be-pulled side, set relative path's base to homedir
		opt.put("RelativeBase", Env.homedir);

		HashMap<String, HashMap<String, String>> local_tree = DirTree.build_dir_tree(local_paths, opt);
		MyLog.append(MyLog.VERBOSE, "local_tree = " + MyGson.toJson(local_tree));
		MyLog.append(MyLog.VERBOSE, "maxsize = " + maxsize);

		// files to delete
		ArrayList<String> deletes = new ArrayList<String>();
		// files needs mode resetting
		Set<String> modes = new HashSet<String>();
		// files can be diff'ed
		Set<String> diff_by_file = new HashSet<String>();
		// files to add or update
		HashMap<String, String> change_by_file = new HashMap<String, String>();
		// files need mtime resetting
		Set<String> need_mtime_reset = new HashSet<String>();
		// files need cksum calculating
		ArrayList<String> need_cksums = new ArrayList<String>();
		// warnings
		ArrayList<String> warns = new ArrayList<String>();
		
		if (!check_mode) {
			warns.add("file mode check is not available");
		}

		// compare localtree with remote_tree
		// sort string keys
		List<String> remote_keys = new ArrayList(remote_tree.keySet());
		Collections.sort(remote_keys);
		for (String k : remote_keys) {
			// k is a relative filename
			if (!local_tree.containsKey(k)) {
				// if the back dir is not shown in local side at all, don't delete it on
				// remote side.
				// for example, assume remote side runs command as
				// $0 remote host port a b
				// if 'a' doesn't exist on the local side, we (local side) should not tell
				// remote to delete b/a on the remote side
				String back = (String) remote_tree.get(k).get("back");
				if (local_tree.containsKey(back)) {
					deletes.add(k);
				}
			}
		}
		// we sort reverse so that files come before their parent dir. This way enables
		// us to copy some (not have to be all) files under a dir
		// https://stackoverflow.com/questions/922528/how-to-sort-map-values-by-key-in-java
		// https://stackoverflow.com/questions/35122490/how-to-reverse-the-order-of-sortedset
		SortedSet<String> reversedlocalFileList = new TreeSet<String>(Collections.reverseOrder());
		reversedlocalFileList.addAll(local_tree.keySet());
		long RequiredSpace = 0;

		for (String k : reversedlocalFileList) {
			// k is a relative filename
			String skipped_message = local_tree.get(k).getOrDefault("skip", null);
			if (skipped_message != null) {
				warns.add("skipped " + k + ": " + skipped_message);
				continue;
			}

			HashMap<String, String> local_node = local_tree.get(k);
			long local_size = Integer.parseInt((String) local_node.get("size"));
			String local_type = (String) local_node.get("type");
			String local_mode = (String) local_node.get("mode");
			String local_mtime = (String) local_node.get("mtime");
			if (!remote_tree.containsKey(k) || !remote_tree.get(k).get("type").equals(local_type)) {
				// remote missing this file or remote is a different type of file: eg, file vs
				// directory
				if (local_size > 0) {
					if (maxsize >= 0 && RequiredSpace + local_size > maxsize) {
						MyLog.append("cutoff before " + k + ": RequiredSpace+local_size(" + RequiredSpace + "+"
								+ local_size + ") > maxsize(" + maxsize + ")");
						break;
					}
					RequiredSpace += local_size;
				}

				if (remote_tree.containsKey(k)) {
					// if remote file exists, it must be of a different type, remove it first and
					// then add.
					change_by_file.put(k, "newType");
					deletes.add(k);
				} else {
					change_by_file.put(k, "add");
				}

				if (local_type.equals("dir")) {
					// We don't tar dir because that would tar up all files under dir.
					// But the problem with this approach is that the dir mode
					// (permission) is then not recorded in the tar file. We will have
					// to keep and send the mode information separately (from the tar file).
					// so is mtime
					if (check_mode) {
						modes.add(k);
					}
					need_mtime_reset.add(k);
				}
				continue;
			}

			// we know both local_tree and remote_tree has the key now.
			HashMap<String, String> remote_node = remote_tree.get(k);
			long remote_size = Integer.parseInt(remote_node.get("size"));
			String remote_mode = remote_node.get("mode");
			String remote_mtime = remote_node.get("mtime");

			// now both sides are same kind type: file, dir, or link
			// we don't modify link's mode
			if (check_mode && !local_type.equals("link") && !remote_mode.equals(local_mode)) {
				modes.add(k);
			}

			// note: dir and link's sizes are hard-coded, so they will always equal.
			// therefore, we are really only compares file's sizes.
			// that is, only files can have different sizes.
			if (remote_size != local_size) {
				if (local_size > 0) {
					if (maxsize >= 0 && RequiredSpace + local_size > maxsize) {
						MyLog.append("cutoff before " + k + ": RequiredSpace+local_size(" + RequiredSpace + "+"
								+ local_size + ") > maxsize(" + maxsize + ")");
						break;
					}
					RequiredSpace += local_size;
				}
				change_by_file.put(k, "update");
				diff_by_file.add(k);
				continue;
			}

			// compare {test} if it is populated
			// dir's {test} and link's {test} are hardcoded, we are really only compare
			// files.
			if (!local_node.containsKey("test") && !remote_node.containsKey("test")) {
				// if both missing tests, we compare mtime first
				// for fast check (default), if size and mtime match, then no need to update.
				// for deep check, or when mtime not matching (but size matching), resort to
				// cksum.
				if (!remote_mtime.equals(local_mtime)) {
					need_cksums.add(k);
					need_mtime_reset.add(k);
				} else if (deep_check != 0) {
					remote_node.put("mtime", local_mtime);
					need_cksums.add(k);
				}
			} else if (!local_node.containsKey("test") || !remote_node.containsKey("test")) {
				// we reach here if only one test is missing.
				// note: if both tests are missing, the previous logic would have already taken
				// care of it.
				// not sure what situation could lead us here yet
				change_by_file.put(k, "update");
			} else if (!local_node.get("test").equals(remote_node.get("test"))) {
				// now both tests exist, we can safely compare
				// not sure what situation will lead us here yet
				change_by_file.put(k, "update");
			} else {
				// local_node.get("test") == remote_node.get("test")
				if (!remote_mtime.equals(local_mtime)) {
					need_mtime_reset.add(k);
				}
			}
		}
	
		String need_cksums_string = "<NEED_CKSUMS>" + String.join("\n", need_cksums) + "</NEED_CKSUMS>";
		MyLog.append("sending need_chksums request to remote: " + need_cksums.size() + " items");
		myconn.writeLine(need_cksums_string);
		myconn.flush();

		MyLog.append("collecting local side cksums: " + need_cksums.size() + " items");
		HashMap<String, String> local_cksum_by_file = Cksum.get_cksums(need_cksums, local_tree);
		MyLog.append("local_cksum_by_file : " + local_cksum_by_file);

		MyLog.append("waiting remote cksum results: ");
		String[] patternArray2 = { "<CKSUM_RESULTS>(.*)</CKSUM_RESULTS>" };
		captures = expectSocket.capture(patternArray2, opt);
		if (captures == null) {
			return;
		}
		String remote_cksum_string = captures.get(0).get(0);

		HashMap<String, String> remote_cksum_by_file = new HashMap<String, String>();
		if (remote_cksum_string != null && remote_cksum_string.length() != 0) {
			Pattern pattern = Pattern.compile("^(.+?)[ ](.+)$");
			for (String line : remote_cksum_string.split("\n")) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					String cksum = matcher.group(1);
					String filename = matcher.group(2);
					remote_cksum_by_file.put(filename, cksum);
				} else {
					MyLog.append(MyLog.ERROR, "bad format at line : '" + line + "'");
				}
			}
		}

		for (String f : local_cksum_by_file.keySet()) {
			if (!remote_cksum_by_file.containsKey(f)) {
				MyLog.append(MyLog.ERROR, "remote cksum missing for " + f);
				change_by_file.put(f, "update");
			} else if (!remote_cksum_by_file.get(f).equals(local_cksum_by_file.get(f))) {
				change_by_file.put(f, "update");
				diff_by_file.add(f); // only type=file can get here.
			} else if (! remote_tree.get(f).get("mtime").equals(local_tree.get(f).get("mtime"))) {
				need_mtime_reset.add(f);
			}
		}
		
		{
			// when we untar to update a file, the file's parent dir's time stamp is
			// updated. we need
			// to restore the time stamp

			Set<String> set = change_by_file.keySet();
			String[] array = set.toArray(new String[0]);
			List<String> list = new ArrayList<String>();
			list.addAll(Arrays.asList(array));
			list.addAll(deletes);

			Pattern parent_dir_pattern = Pattern.compile("^(.+)/");
			for (String k : list) {
				Matcher parent_dir_matcher = parent_dir_pattern.matcher(k);
				if (parent_dir_matcher.find()) {
					String parent_dir = parent_dir_matcher.group(1);
					need_mtime_reset.add(parent_dir);
				}
			}
		}

		String deletes_string = "<DELETES>" + String.join("\n", deletes) + "</DELETES>";
		MyLog.append("sending deletes : " + deletes.size() + " items");
		MyLog.append(MyLog.VERBOSE, "   " + deletes_string);
		myconn.writeLine(deletes_string);

		String adds_string = StrBlda.build_string(change_by_file, "%6s %s\n", "ADDS");
		MyLog.append("sending adds : " + change_by_file.size() + " items");
		MyLog.append(MyLog.VERBOSE, "   " + adds_string);
		myconn.writeLine(adds_string);
	
		String mtimes_string = StrBlda.build_string(local_tree, "%s %s\n", "MTIMES", "mtime", need_mtime_reset);
		MyLog.append("sending mtimes : " + need_mtime_reset.size() + " items");
		MyLog.append(MyLog.VERBOSE, "   " + mtimes_string);
		myconn.writeLine(mtimes_string);

		String modes_string = StrBlda.build_string(local_tree, "%s %s\n", "MODES", "mode", modes);
		MyLog.append("sending modes : " + modes.size() + " items");	
		MyLog.append(MyLog.VERBOSE, "   " + modes_string);
		myconn.writeLine(modes_string);

		String warns_string = "<WARNS>" + String.join("\n", warns) + "</WARNS>";
		MyLog.append("sending warns : " + warns.size() + " items");
		MyLog.append(MyLog.VERBOSE, "   " + warns_string);
		myconn.writeLine(warns_string);

		String space_string = "<SPACE>" + RequiredSpace + "</SPACE>";
		MyLog.append("sending RequiredSpace : " + RequiredSpace + " bytes");
		MyLog.append(MyLog.VERBOSE, "   " + space_string);
		myconn.writeLine(space_string);

		myconn.flush();

		MyLog.append("waiting for transfer mode from remote: ");
		String[] patternArray3 = { "please send (data|diff|unpacked)" };
		captures = expectSocket.capture(patternArray3, opt);
		if (captures == null) {
			return;
		}
		String mode = captures.get(0).get(0);

		String tmp_tar_file = TmpFile.createTmpFile(Env.tmpBase, "tpdist", opt);
		
		MyLog.append("received remote tranfer mode: " + mode + ". creating local tar file" + tmp_tar_file);
		if (!mode.equals("data") && !mode.equals("diff")) {
			String message = "tranfer mode '" + mode + "' is not supported";
			MyLog.append(message);
			myconn.writeLine(message);
			return;
		}
		
		ArrayList<String> files_to_tar = new ArrayList<String>();
		if (mode.equals("diff")) {
			files_to_tar.addAll(diff_by_file);
		} else {
			files_to_tar.addAll(change_by_file.keySet());
		}
		
		if (files_to_tar.size() == 0) {
			MyLog.append("no need to send anything to remote");
			return;
		}
		
		HashMap<String, ArrayList<String>> files_by_front = new HashMap<String, ArrayList<String>>();
		for (String f : files_to_tar) {
			String front = (String) local_tree.get(f).get("front");
			if (local_tree.get(f).get("type").equals("dir")) {
				// Skip non-empty dir because tar'ing dir will also tar the files underneath.
				// But we include empty dir
				File dir = new File(front + '/' + f);
				if (dir.list().length != 0) {
					continue;
				}
			}
			if (!files_by_front.containsKey(front)) {
				ArrayList<String> files = new ArrayList<String>();
				files_by_front.put(front, files);
			}
			files_by_front.get(front).add(f);
		}
		MyLog.append(MyLog.VERBOSE, "files_by_front = " + MyGson.toJson(files_by_front));
		try {
			TarArchiveOutputStream tarArchiveOutputStream = null;
			for (String front : files_by_front.keySet()) {
				tarArchiveOutputStream = Tar.createTar(tmp_tar_file, front, files_by_front.get(front), true,
						tarArchiveOutputStream);
				tarArchiveOutputStream.close();
			}
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, ExceptionUtils.getRootCauseMessage(e));
			return;
		}
		
		MyLog.append("sending  tar-format data (mode=" + mode + ") to remote");
		
		// use blocked io to send data, therefore use java OutputStream
		int tar_size = 0;
		try {
			// https://docs.oracle.com/javase/6/docs/api/java/net/Socket.html#getOutputStream()
			// https://stackoverflow.com/questions/9046820/fastest-way-to-incrementally-read-a-large-file
			InputStream inputStream = new FileInputStream(tmp_tar_file);
			// https://stackoverflow.com/questions/9046820/fastest-way-to-incrementally-read-a-large-file
			byte buffer[] = new byte[1024 * 1024];
			int size;
			while ((size = inputStream.read(buffer)) != -1) {
				MyLog.append("   bytes read and sent = " + size);
				myconn.write(buffer, 0, size);
				tar_size += size;
			}
            myconn.flush();
            try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				MyLog.append(MyLog.ERROR, ExceptionUtils.getRootCauseMessage(e));
			}
			inputStream.close();
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, ExceptionUtils.getRootCauseMessage(e));
			return;
		}
		MyLog.append("sent tar_size=" + tar_size + ". closed remote connection");
		
		if ((Boolean) opt.getOrDefault("KeepTmpFile", false)) {
			MyLog.append("tmp file " + tmp_tar_file + " is kept");
		} else {
			MyLog.append("removing tmp file " + tmp_tar_file);
			try {
				FileUtils.forceDelete(new File(tmp_tar_file));
			} catch (IOException e) {
				MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e));
			}
		}
		MyLog.append("\n\n");
		return;
	}
}
