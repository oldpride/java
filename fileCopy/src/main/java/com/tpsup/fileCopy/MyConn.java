package com.tpsup.fileCopy;

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public final class MyConn {
    public Socket socket;
    public int in_count = 0;    // total bytes received
    public int  out_count = 0;  // total bytes sent
    public String key = null;   // encrypt key 
    public OutputStreamWriter writer;   
    public MyCoder in_coder = null;  // encoder for incoming
    public MyCoder out_coder = null; // encoder for outgoing
    public SocketChannel socketChannel = null;
       
    public MyConn(Socket socket, String key) {
        this.socket = socket;
        this.key = key;
        this.socketChannel = socket.getChannel();
        
        if (key != null && !key.isEmpty()) {
            this.in_coder = new MyCoder(key);
            this.out_coder = new MyCoder(key);
        }     

        try {
            this.writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
