package com.tpsup.tpdist;

// we don't use log4j or anything similar to avoid a configuration file. this is for easy deploy.

import javax.swing.JTextArea;

public final class MyLog {
	public static JTextArea jtextarea = null;

	public static enum Level {
		ERROR, INFO, VERBOSE
	}

	// syntax sugar
	final public static Level ERROR = Level.ERROR;
	final public static Level INFO = Level.INFO;
	final public static Level VERBOSE = Level.VERBOSE;

	public static void append(Level level, String msg) {
		if (msg == null) {
			return;
		}
		
		StackTraceElement caller = Thread.currentThread().getStackTrace()[1];
		if (caller.getClassName().contains("MyLog")) {
			caller = Thread.currentThread().getStackTrace()[2];
		}
		if (caller.getClassName().contains("MyLog")) {
			caller = Thread.currentThread().getStackTrace()[3];
		}
		String prefix = caller.getClassName() + ":" + caller.getLineNumber();
		
		if (level == Level.ERROR) {
			// always print error
			System.err.println(prefix + " " + msg);
			if (jtextarea != null) {
				jtextarea.append(prefix + " " + msg);
			}
		} else if (Env.verbose || level == Level.INFO) {
			// if in VERBOSE mode, print everything

			System.out.println(prefix + " " + msg);
			if (jtextarea != null) {
				jtextarea.append(prefix + " " + msg);
			}
		}
	}

	public static void append(String msg) {
		append(Level.INFO, msg);
	}
}
