package com.tpsup.fileCopy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;

public class ExpectSocket {
	private MyConn myconn = null;
	int ExpectTimeout = -1; // seconds

	public ExpectSocket(MyConn myconn, HashMap<String, Object> opt) {
		this.myconn = myconn;
		if (opt == null) {
			opt = new HashMap<String, Object>();
		}
		this.ExpectTimeout = (Integer) opt.getOrDefault("ExpectTimeout", this.ExpectTimeout);
	}

	// total_string = <VERSION>6.3</VERSION>
	// <PATH>C:/Users/Public/Documents/CYGWIN/home/hantian/testdir
	// C:/Users/Public/Documents/CYGWIN/home/hantian/testdir2</PATH><DEEP>0</DEEP><TREE>
	// key=testdir2|back=testdir2|front=/home/axptsusu/|mode=0755|mtime=1546436740|size=128|test=dir|type=dir
	// key=testdir2/e.txt|back=testdir2|front=/home/axptsusu/|mode=0644|mtime=1546436740|size=5|type=file
	// </TREE><MAXSIZE>-l</MAXSIZE><EXCLUDE></EXCLUDE><MATCH></MATCH>
	public ArrayList<ArrayList<String>> capture(String[] patterns, HashMap<String, Object> opt) {
		ArrayList<ArrayList<String>> captures = new ArrayList<ArrayList<String>>();
		StringBuilder bld = new StringBuilder();
		int total_wait = 0; // seconds
		int this_section_recv = 0; // number of bytes
		String error_message = null;

		// https://stackoverflow.com/questions/12756360/how-to-make-java-set/12756499
		Set<Integer> matched = new HashSet<Integer>();

		int ExpectTimeout = (Integer) opt.getOrDefault("ExpectTimeout", this.ExpectTimeout);

		ArrayList<Pattern> compiled_patterns = new ArrayList<Pattern>();
		for (String pattern : patterns) {
			Pattern compiled_pattern = Pattern.compile(pattern, Pattern.DOTALL);
			compiled_patterns.add(compiled_pattern);
		}

		try {
			// turn off blocking (buffer) before reading out data
			this.myconn.configureBlocking(false);

			while (true) {
				String new_data = this.myconn.NioReadString();
				if (new_data == null) {
					error_message = "remote side closed socket";
					break;
				} else if (new_data.isEmpty()) {
					// no new data
					if (total_wait > ExpectTimeout) {
						error_message = "timed out after " + ExpectTimeout
								+ " seconds. very likely wrong protocol. expecting " + Version.expected_protocol + ".*";
						myconn.writeString(error_message);
						break;
					}
					Thread.sleep(1000); // 1000 milliseconds is one second.
					total_wait++;
					continue;
				}

				this_section_recv += myconn.last_in;

				MyLogger.append("received " + myconn.last_in + " byte(s), total=" + myconn.total_in
						+ ", this section so far " + this_section_recv + " byte(s)" + bld.toString());

				// https://www.geeksforgeeks.org/array-vs-arraylist-in-java/
				// ArrayList<String> captures = new ArrayList<String>();
				Boolean all_matched = true;
				for (int i = 0; i < compiled_patterns.size(); i++) {
					if (matched.contains(i)) {
						continue;
					}
					// https://stackoverflow.com/questions/17969436/java-regex-capturing-groups
					Pattern pattern = compiled_patterns.get(i);
					Matcher matcher = pattern.matcher(bld.toString());

					if (matcher.find()) {
						captures.add(i, new ArrayList<String>());
						captures.get(i).add(matcher.group(1));
						captures.get(i).add(matcher.group(2));
						captures.get(i).add(matcher.group(3));
						MyLogger.append("matched pattern= " + patterns[i]);
						matched.add(i);
					} else {
						all_matched = false;
					}
				}
				if (all_matched) {
					MyLogger.append("received complete information from remote");
					return captures;
				}
			}
		} catch (Exception e) {
			error_message = e.getStackTrace().toString();
		}

		if (error_message != null) {
			MyLogger.append(error_message);
			MyLogger.append("matched so far");
			for (int i = 0; i < patterns.length; i++) {
				String line = "   pattern=" + patterns[i] + "  ";
				if (matched.contains(i)) {
					line += "matched";
				} else {
					line += "not matched";
				}
				MyLogger.append(line);
			}

			String data = bld.toString();
			int data_size = data.length();
			int print_size = data_size;
			if (print_size > 100) {
				MyLogger.append("   Last words: " + data.substring(data_size - 100));
			} else {
				MyLogger.append("   Last words: " + data);
			}
		}

		return null;
	}
}
