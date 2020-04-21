package com.tpsup.tpdist;

public final class Env {
	final public static String uname = System.getProperty("os.name");
	final public static boolean isUnix = uname.toLowerCase().contains("unix|ux|cygwin|mingw");
	final public static boolean isWindows = uname.toLowerCase().contains("windows");
	
	public static boolean verbose = false;
	
	public static void main(String[] args) {
		MyLog.append("uname = " + uname);
		MyLog.append("isUnix = " + isUnix);
		MyLog.append("isWindows = " + isWindows);
	}
}
