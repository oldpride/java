package com.tpsup.tpdist;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class Client {
	MyConn myconn = null;

	public Client(String host, int port, HashMap<String, Object> opt) {
//		SocketChannel socketchannel;
//		try {
//			socketchannel = SocketChannel.open();
//		} catch (IOException e) {
//			MyLog.append(MyLog.ERROR, e.getMessage());
//			return;
//		}
//		Socket socket = socketchannel.socket();
		InetSocketAddress address = new InetSocketAddress(host, port);
		int maxtry = (Integer) opt.getOrDefault("maxtry", 5);
		String key = (String) opt.getOrDefault("encode", null);
		int interval = 5;
		for (int i = 0; i < maxtry; i++) {
			try {
				Socket socket = new Socket();
				socket.connect(address, 3000);
				myconn = new MyConn(socket, key);
				return;
			} catch (IOException e) {
				MyLog.append(MyLog.ERROR, e.getMessage());
			}

			if (i + 1 == maxtry) {
				// no need to sleep after last round
				break;
			}
			MyLog.append("On $host please run");
			MyLog.append("   tpdist server " + port);
			MyLog.append("   Will retry in " + interval + " seconds for " + (maxtry - i - 1) + " time(s)");

			try {
				Thread.sleep(interval * 1000);
			} catch (InterruptedException e) {
				MyLog.append("^C");
				// if we are Control+C'ed, return
				return;
			}
		}

		MyLog.append("failed to connect after " + maxtry + "times");
	}
}
