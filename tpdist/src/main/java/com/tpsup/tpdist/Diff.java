package com.tpsup.tpdist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Diff {	
	public static int diff(String arg_string, String f1, String f2) {
		// https://stackoverflow.com/questions/29545611/executing-powershell-commands-in-java-program
		Process p;
		String command = null;
		if (Env.isUnix) {
			command = "diff " + arg_string + " " + f1 + " " + f2;
		} else if (Env.isWindows){
			// diff (cat "file 1") (cat "file 2")
			command = "powershell.exe diff " + arg_string + " (cat '" + f1 + "') (cat '" + f2 + "')";
		} else {
			MyLog.append(MyLog.ERROR, "unsupported uname= " + Env.uname);
			return 1;
		}
		MyLog.append(MyLog.VERBOSE, command);
		try {
			p = Runtime.getRuntime().exec(command);
			p.getOutputStream().close();
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
			return 1;
		}		
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		try {
			while ((s = stdInput.readLine()) != null) {
				MyLog.append(s);
			}
			p.getInputStream().close();
			p.waitFor();
			return p.exitValue();			
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
			return 1;
		} catch (InterruptedException e) {
			MyLog.append(MyLog.ERROR, e.getStackTrace().toString());
			return 1;
		}
	}
	
	public static int diff(String f1, String f2) {
		return diff("", f1, f2);
	}
	
	public static void main(String[] args) {
		Env.verbose = true;
		diff("C:/users/william/github/tpsup/ps1/tpdist.ps1", "C:/users/william/tmp/ps1/tpdist.ps1");
		MyLog.append(MyLog.VERBOSE, "done");
	}
}
