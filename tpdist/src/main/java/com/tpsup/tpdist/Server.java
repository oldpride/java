package com.tpsup.tpdist;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class Server {

	public static MyConn listenAndAccept(int port, HashMap<String, Object>opt) {
        int idle = (Integer) opt.getOrDefault("idle", 600);
        String key = (String) opt.getOrDefault("encode", null);
  
        try {
            // Instead of creating a ServerSocket, create a ServerSocketChannel
        	ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        	
            // Get the Socket connected to this channel, and bind it to the listening port
            ServerSocket serverSocket = serverSocketChannel.socket();         
            serverSocket.setReuseAddress(true);
            InetSocketAddress isa = new InetSocketAddress(port);
            serverSocket.bind(isa);
            
            serverSocket.setSoTimeout(idle * 1000);
            MyLog.append("listener started at port " + port + ". waiting for " + idle + " seconds" );
            SocketChannel clientChannel = serverSocketChannel.accept(); // this thread is blocked here
 
            // close the listening port, as we only accept one connection
            serverSocketChannel.close();
            serverSocket.close();       
            
            MyConn myconn = new MyConn(clientChannel.socket(), key);
            return myconn;
        } catch (IOException e) {
            MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e));
        }
        
        return null;
	}
}