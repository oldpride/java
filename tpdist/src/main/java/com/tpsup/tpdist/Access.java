package com.tpsup.tpdist;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Access {
	HashMap<String, HashMap<String, ArrayList<String>>> matrix = null;	
	HashMap<String, MatchExclude> allowDeny = null;

	public Access(HashMap<String, Object> opt) throws Exception {
		this.matrix = new HashMap<String, HashMap<String, ArrayList<String>>>();
		this.allowDeny = new HashMap<String, MatchExclude>();
		String[] types = { "host", "file" };
		String[] accesses = { "allow", "deny" };

		Pattern blanklinePattern = Pattern.compile("^\\s+$");
		Pattern commentPattern = Pattern.compile("^\\s+#");

		for (String type : types) {
			HashMap<String, ArrayList<String>> typeMap = new HashMap<String, ArrayList<String>>();
			this.matrix.put(type, typeMap);
			for (String access : accesses) {
				ArrayList<String> strings = new ArrayList<String>();
				typeMap.put(access, strings);
				
				String key = access + type; // eg, allow + host = allowhost
				String file = (String) opt.getOrDefault(key, null);
				if (file == null) {
					file = Env.homedir + ".tpsup/tpdist_" + key + ".txt";
					if (!(new File(file).exists())) {
						continue;
					}
				} else {
					if (!(new File(file).exists())) {
						throw new Exception(file + " not found");
					}
				}
				
				// https://www.baeldung.com/java-file-to-arraylist
				Charset charset = Charset.forName("utf-8");
				List<String> lines = Files.readAllLines(Paths.get(file), charset);
				for (String line : lines) {
					if (blanklinePattern.matcher(line).find()) {
						continue;
					}
					if (commentPattern.matcher(line).find()) {
						continue;
					}
					line = line.trim(); // trim() removes space, tab, newline from both ends
					strings.add(line);
				}

				if (strings.size() == 0) {
					// why fatal here?
					// because if a file exists but is empty, for example, if the allow file exists
					// but is
					// empty, should we allow all or allow none? this becomes ambiguous and prone to
					// error!!
					throw new Exception("FATAL: there is no settings in " + file
							+ ". It may mean 'allow all' or 'deny all'. To avoid ambiguity, either remove file or add settings.");
				}
				typeMap.put(access, strings);
			}
			
			String[] allowArray = typeMap.get("allow").toArray(new String[0]);
			String[] denyArray = typeMap.get("deny").toArray(new String[0]);
			MyLog.append(MyLog.VERBOSE, MyGson.toJson(typeMap));
			MatchExclude matchExclude = new MatchExclude(allowArray, denyArray);
			this.allowDeny.put(type, matchExclude);
		}
	}
	
	public boolean is_allowed (String type, String subject) {
		return this.allowDeny.get(type).pass(subject);		
	}
}
