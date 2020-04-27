package com.tpsup.tpdist;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class Client {
	public static MyConn connnect(String host, int port, HashMap<String, Object> opt) {
		InetSocketAddress address = new InetSocketAddress(host, port);
		int maxtry = (Integer) opt.getOrDefault("maxtry", 5);
		String key = (String) opt.getOrDefault("encode", null);
		int interval = 5;

		for (int i = 0; i < maxtry; i++) {
			try {
				// to use Java Nonblocking IO, we must open SocketChannel (NIO) first, then
				// get Socket from it.
				// If we create Socket first then get SocketChannel from Socket.GetChannel(),
				// the SocketChannel will be null
	            SocketChannel socketchannel = SocketChannel.open();
	            Socket socket = socketchannel.socket();
				socket.connect(address, 3000); //time out after 3 seconds.
				MyConn myconn = new MyConn(socket, key);
				return myconn;
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
				return null;
			}
		}

		MyLog.append("failed to connect after " + maxtry + "times");
		
		return null;
	}
}
