package com.tpsup.tpdist;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class StrBlda {
	private static String build_string(Object map, String format, String tag, String attr, Set<String> set) {

		HashMap<String, String> map2 = null;
		HashMap<String, HashMap<String, String>> map3 = null;
		Collection<String> sorted = new TreeSet<String>();
		
		if (attr == null) {
			map2 = (HashMap<String, String>) map;			
		} else {
			map3 = (HashMap<String, HashMap<String, String>>) map;
		}

		if (set == null) {
			if (attr == null) {			
				sorted.addAll(map2.keySet());
			} else {
				sorted.addAll(map3.keySet());
			}
		} else {
			sorted.addAll(set);
		}
		
		StringBuilder bld = new StringBuilder();
		bld.append("<" + tag + ">");
		for (String f : sorted) {
			if (attr == null) {
				bld.append(String.format(format, map2.get(f), f));
			} else {
				bld.append(String.format(format, map3.get(f).get(attr), f));
			}
		}
		bld.append("</" + tag + ">");
		return bld.toString();
	}

	public static String build_string(HashMap<String, HashMap<String, String>> map, String format, String tag,
			String attr, Set<String>set) {
		return build_string((Object) map, format, tag, attr, set);
	}
	
	public static String build_string(HashMap<String, HashMap<String, String>> map, String format, String tag,
			String attr) {
		return build_string((Object) map, format, tag, attr, null);
	}

	public static String build_string(HashMap<String, String> map, String format, String tag) {
		return build_string((Object) map, format, tag, null, null);
	}

	public static void main(String[] args) {
		HashMap<String, String> hashmap = new HashMap<String, String>();
		hashmap.put("a", "1");
		hashmap.put("b", "2");
		hashmap.put("c", "3");
		MyLog.append("hashmap = " + build_string(hashmap, "%6s %s\n", "ADD"));

		HashMap<String, HashMap<String, String>> hashhash = new HashMap<String, HashMap<String, String>>();

		hashhash.put("a", hashmap);
		hashhash.put("b", new HashMap<String, String>());
		hashhash.get("b").put("a", "hashhash-b-a");

		MyLog.append("hashhash = " + build_string(hashhash, "%6s %s\n", "ADD", "a"));
	}
}
