package com.tpsup.tpdist;

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class StrBlda {
	public static String build_string(Object map, String format, String tag,
			String attr) {

		HashMap<String, String> map2 = null;
		HashMap<String, HashMap<String,String>> map3 = null;
		Collection<String> sorted = new TreeSet<String>();
		if (attr == null) {
			map2 = (HashMap<String, String>) map;
			sorted.addAll(map2.keySet());
		} else {
			map3 = (HashMap<String, HashMap<String,String>>) map;
			sorted.addAll(map3.keySet());
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

	public static String build_string(HashMap<String, String> map, String format, String tag) {
		return build_string((Object)map,format, tag, null);
	}
	
	public static void main(String[] args) {
		HashMap<String, String> hashmap = new HashMap<String, String>();		
		hashmap.put("a", "1");
		hashmap.put("b", "2");
		hashmap.put("c", "3");		
		MyLog.append("hashmap = " + build_string(hashmap, "%6s %s\n", "ADD"));
	
		HashMap<String, HashMap<String, String>> hashhash = new HashMap<String, HashMap<String,String>>();
		
		hashhash.put("a", hashmap);
		hashhash.put("b", new HashMap<String, String>());
		hashhash.get("b").put("a", "hashhash-b-a");
		
		MyLog.append("hashhash = " + build_string(hashhash, "%6s %s\n", "ADD", "a"));
	}
}
