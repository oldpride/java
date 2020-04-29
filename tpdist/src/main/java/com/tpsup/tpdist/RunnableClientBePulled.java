package com.tpsup.tpdist;

import java.util.HashMap;

public class RunnableClientBePulled implements Runnable {
	String host;
	int port;
	HashMap<String, Object> opt;

	public RunnableClientBePulled(String host, int port, HashMap<String, Object> opt) {
		this.host = host;
		this.port = port;
		this.opt = opt;
	}

	public RunnableClientBePulled(HashMap<String, Object> client_params, HashMap<String, Object> opt) {
		// https://stackoverflow.com/questions/285177/how-do-i-call-one-constructor-from-another-in-java
		this((String) client_params.get("host"), (Integer) client_params.get("port"), opt);
	}

	public void run() {
		MyConn myconn = Client.connnect(host, port, opt);
		if (myconn == null) {
			return;
		}
		ToBePulled.bePulled(myconn, opt);
		myconn.close();
	}
}
