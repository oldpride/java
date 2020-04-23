package com.tpsup.tpdist;

public final class Env {
	final public static String uname = System.getProperty("os.name");
	final public static boolean isUnix = uname.toLowerCase().contains("unix|ux");
	final public static boolean isWindows = uname.toLowerCase().contains("windows");
	public static String tmpBase;
	
	// static class initializer
	static {
		if (isWindows) {
			// C:\Users\william\AppData\Local\Temp\tpsup, this is owned by the user
			tmpBase = System.getProperty("java.io.tmpdir") + "tpsup";
		} else {
			// /var/tmp, /tmp, these are shared among users
			String username = System.getProperty("user.name");
			tmpBase = System.getProperty("java.io.tmpdir") + "tmp_" + username;
		}
	}
	
	public static boolean verbose = false;
	
	public static void main(String[] args) {
		MyLog.append("uname = " + uname);
		MyLog.append("isUnix = " + isUnix);
		MyLog.append("isWindows = " + isWindows);
		MyLog.append("tmpBase = " + tmpBase);
	}
}
