package com.tpsup.tpdist;

// we don't use log4j or anything similar to avoid a configuration file. this is for easy deploy.

import javax.swing.JTextArea;

public final class MyLog {
	public static JTextArea jtextarea = null;
	public static boolean verbose = Env.verbose;

	public static enum Level {
		ERROR, INFO, VERBOSE
	}

	// syntax sugar
	final public static Level ERROR = Level.ERROR;
	final public static Level INFO = Level.INFO;
	final public static Level VERBOSE = Level.VERBOSE;

	public static void append(Level level, String msg) {
		if (level == Level.ERROR) {
			// always print error
			System.err.println(msg);
			if (jtextarea != null) {
				jtextarea.append(msg);
			}
		} else if (verbose || level == Level.INFO) {
			// if in VERBOSE mode, print everything
			System.out.println(msg);
			if (jtextarea != null) {
				jtextarea.append(msg);
			}
		}
	}

	public static void append(String msg) {
		append(Level.INFO, msg);
	}
}
