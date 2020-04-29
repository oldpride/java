package com.tpsup.tpdist;

import java.util.HashMap;

public class RunnableServerBePulled implements Runnable {
	int port;
	HashMap<String, Object> opt;

	public RunnableServerBePulled(int port, HashMap<String, Object> opt) {
		this.port = port;
		this.opt = opt;
	}

	public RunnableServerBePulled(HashMap<String, Object> server_params, HashMap<String, Object> opt) {
		// https://stackoverflow.com/questions/285177/how-do-i-call-one-constructor-from-another-in-java
		this((Integer) server_params.get("port"), opt);
	}

	public void run() {
		MyConn myconn = Server.listenAndAccept(port, opt);
		if (myconn == null) {
			return;
		}
		ToBePulled.bePulled(myconn, opt);
		myconn.close();
	}
}
