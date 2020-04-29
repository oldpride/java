package com.tpsup.tpdist;

public final class Env {
	final public static String projName = "tpdist";
	final public static String homedir = System.getProperty("user.home");
	final public static String uname = System.getProperty("os.name");
	final public static boolean isUnix = uname.toLowerCase().contains("unix|ux");
	final public static boolean isWindows = uname.toLowerCase().contains("windows");
	final public static String version = "7.0";
	public static String mainVersion;
	public static String expected_peer_protocol;

	public static String tmpBase;

	// static class initializer
	static {
		String[] version_split = version.split("[.]");
		mainVersion = version_split[0];
		expected_peer_protocol = mainVersion;
						
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
