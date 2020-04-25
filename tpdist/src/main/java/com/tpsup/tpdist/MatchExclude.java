package com.tpsup.tpdist;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchExclude {
	private ArrayList<Pattern> matches;
	private ArrayList<Pattern> excludes;
	public String matches_string;
	public String excludes_string;
	public String delimiter_string;

	public MatchExclude(String matches_string, String excludes_string, String delimiter_string) {
		this.matches_string = matches_string;
		this.excludes_string = excludes_string;
		this.delimiter_string = delimiter_string;
		this.matches = new ArrayList<Pattern>();
		for (String p : matches_string.split(delimiter_string)) {
			if (p.isEmpty()) {
				continue;
			}
			this.matches.add(Pattern.compile(p));
		}

		this.excludes = new ArrayList<Pattern>();
		for (String p : excludes_string.split(delimiter_string)) {
			if (p.isEmpty()) {
				continue;
			}
			this.excludes.add(Pattern.compile(p));
		}
	}

	public MatchExclude(String[] matchesArray, String[] excludesArray) {
		this.matches = new ArrayList<Pattern>();
		for (String p : matchesArray) {
			if (p.isEmpty()) {
				continue;
			}
			this.matches.add(Pattern.compile(p));
		}

		this.excludes = new ArrayList<Pattern>();
		for (String p : excludesArray) {
			if (p.isEmpty()) {
				continue;
			}
			this.excludes.add(Pattern.compile(p));
		}
	}

	public boolean pass(String string) {
		if (this.matches.size() > 0) {
			boolean matched = false;
			for (Pattern p : this.matches) {
				Matcher m = p.matcher(string);
				if (m.find()) {
					matched = true;
					break;
				}
			}

			if (!matched) {
				return false;
			}
		}

		if (this.excludes.size() > 0) {
			for (Pattern p : this.excludes) {
				Matcher m = p.matcher(string);
				if (m.find()) {
					return false;
				}
			}
		}
		return true;
	}
}
