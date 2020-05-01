package com.tpsup.tpdist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ExpectSocket {
	private MyConn myconn = null;
	int ExpectTimeout = -1; // seconds

	public ExpectSocket(MyConn myconn, HashMap<String, Object> opt) {
		this.myconn = myconn;
		this.ExpectTimeout = (Integer) opt.getOrDefault("ExpectTimeout", this.ExpectTimeout);
	}

	// total_string = <VERSION>6.3</VERSION>
	// <PATH>C:/Users/Public/Documents/CYGWIN/home/hantian/testdir
	// C:/Users/Public/Documents/CYGWIN/home/hantian/testdir2</PATH><DEEP>0</DEEP><TREE>
	// key=testdir2|back=testdir2|front=/home/axptsusu/|mode=0755|mtime=1546436740|size=128|test=dir|type=dir
	// key=testdir2/e.txt|back=testdir2|front=/home/axptsusu/|mode=0644|mtime=1546436740|size=5|type=file
	// </TREE><MAXSIZE>-l</MAXSIZE><EXCLUDE></EXCLUDE><MATCH></MATCH>
	public ArrayList<ArrayList<String>> capture(String[] patterns, HashMap<String, Object> opt) {
		StringBuilder bld = new StringBuilder();
		int total_wait = 0; // seconds
		int this_section_recv = 0; // number of bytes
		String error_message = null;

		// https://stackoverflow.com/questions/12756360/how-to-make-java-set/12756499
		Set<Integer> matched = new HashSet<Integer>();

		int ExpectTimeout = (Integer) opt.getOrDefault("ExpectTimeout", this.ExpectTimeout);

		ArrayList<ArrayList<String>> captures = new ArrayList<ArrayList<String>>();
		ArrayList<Pattern> compiled_patterns = new ArrayList<Pattern>();
		for (String pattern : patterns) {
			// compile patterns
			Pattern compiled_pattern = Pattern.compile(pattern, Pattern.DOTALL);
			compiled_patterns.add(compiled_pattern);
			// initialize captures array
			captures.add(new ArrayList<String>());
		}

		try {
			while (true) {
				String new_data = this.myconn.NioReadString(); // non-blocking IO, need to use sleep to throttle
				if (new_data == null) {
					error_message = "remote side closed socket";
					break;
				} else if (new_data.isEmpty()) {
					// no new data
					int sleep = 2;
					if (total_wait > ExpectTimeout) {
						error_message = "timed out after " + ExpectTimeout
								+ " seconds. very likely wrong protocol. expecting " + Env.expected_peer_protocol
								+ ".*";
						myconn.writeLine(error_message);
						break;
					}
					Thread.sleep(sleep*1000); // 1000 milliseconds is one second.
					total_wait += sleep;
					continue;
				}

				this_section_recv += myconn.last_in;
				bld.append(new_data);

				MyLog.append("received " + myconn.last_in + " byte(s), total=" + myconn.total_in
						+ ", this section so far " + this_section_recv + " byte(s).");
				MyLog.append(MyLog.VERBOSE, bld.toString());

				// https://www.geeksforgeeks.org/array-vs-arraylist-in-java/
				// ArrayList<String> captures = new ArrayList<String>();
				Boolean all_matched = true;
				String data_so_far = bld.toString();
				for (int i = 0; i < compiled_patterns.size(); i++) {
					if (matched.contains(i)) {
						continue;
					}
					// https://stackoverflow.com/questions/17969436/java-regex-capturing-groups
					Pattern pattern = compiled_patterns.get(i);
					Matcher matcher = pattern.matcher(data_so_far);
					if (matcher.find()) {
						for (int j = 0; j < matcher.groupCount(); j++) {
							captures.get(i).add(matcher.group(j + 1));
						}
						MyLog.append("matched pattern= " + patterns[i]);
						matched.add(i);
					} else {
						all_matched = false;
					}
				}
				if (all_matched) {
					MyLog.append("received complete information from remote");
					return captures;
				}
				// add a little throttle, a second, to reduce the number of loops.
				// this makes the log looks nicer
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			error_message = ExceptionUtils.getStackTrace(e);
		}

		if (error_message != null) {
			MyLog.append(MyLog.ERROR, error_message);
			MyLog.append("matched so far");
			for (int i = 0; i < patterns.length; i++) {
				String line = "   pattern=" + patterns[i] + "  ";
				if (matched.contains(i)) {
					line += "matched";
				} else {
					line += "not matched";
				}
				MyLog.append(line);
			}

			String data = bld.toString();
			int data_size = data.length();
			int print_size = data_size;
			if (print_size > 100) {
				MyLog.append("   Last words: " + data.substring(data_size - 100));
			} else {
				MyLog.append("   Last words: " + data);
			}
		}
		return null;
	}
}