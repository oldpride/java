package com.tpsup.tpdist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class RunnableServerPull implements Runnable {
	int port;
	ArrayList<String> remote_paths;
	String local_dir;
	HashMap<String, Object> opt;

	public RunnableServerPull(int port, ArrayList<String> remote_paths, String local_dir,
			HashMap<String, Object> opt) {
		this.port = port;
		this.remote_paths = remote_paths;
		this.local_dir = local_dir;
		this.opt = opt;
	}

	public RunnableServerPull(HashMap<String, Object> server_params, HashMap<String, Object> pull_params,
			HashMap<String, Object> opt) {
		// https://stackoverflow.com/questions/285177/how-do-i-call-one-constructor-from-another-in-java
		this((Integer) server_params.get("port"),
				(new ArrayList<String>(Arrays.asList((String[]) pull_params.get("remote_paths")))), (String) pull_params.get("local_dir"), opt);
	}

	public void run() {
		MyConn myconn = Server.listenAndAccept(port, opt);
		if (myconn == null) {
			return;
		}
		ToPull.pull(myconn, remote_paths, local_dir, opt);
		myconn.close();
	}
}
