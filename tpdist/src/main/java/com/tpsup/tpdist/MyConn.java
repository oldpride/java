package com.tpsup.tpdist;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public final class MyConn {
	public int total_in = 0; // total bytes received
	public int total_out = 0; // total bytes sent

	public int last_in = 0; // number of last bytes received
	public int last_out = 0; // number of last bytes sent
	
	public MyCoder in_coder = null; // encoder for incoming
	public MyCoder out_coder = null; // encoder for outgoing
	public String key = null; // encrypt key

	// Design guideline:
	// https://stackoverflow.com/questions/14225957/socket-vs-socketchannel
	// Socket (InputStream/OutputStream) is blocked. We need blocking when writing out data,
	// therefore, our output goes through Socket.OutputStream.
	// SocketChannel (Java NIO) is non-blocking. We need non-blocking when reading data,
	// therefore, our input goes through SocketChannel

	// java byte=8 bits, char=16bits
	// use OutputStream (writes bytes) instead of OutputStreamWriter (writes char)
	public Socket socket = null;
	public OutputStream outstream = null; // for output, this writes bytes, blocking
	public SocketChannel socketChannel = null; // for input, this reads bytes, nonblocking (NIO)
	
	public ByteBuffer buffer = null;

	public MyConn(Socket socket, String key) {
		this.socket = socket;	
		MyLog.append(MyLog.VERBOSE, "socket = " + socket.toString());
		
		this.socketChannel = socket.getChannel();
		MyLog.append(MyLog.VERBOSE, "socketChannel = " + this.socketChannel.toString());
		try {
			this.outstream = socket.getOutputStream();
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getMessage());
		}
		
		this.key = key;
		if (key != null && !key.isEmpty()) {
			this.in_coder = new MyCoder(key);
			this.out_coder = new MyCoder(key);
		}
		
		this.buffer = ByteBuffer.allocate(1024 * 1024);
	}

	public void write(byte[] data, int size) {
		try {
			if (this.out_coder != null) {
				this.outstream.write(this.out_coder.xor(data, size), 0, size);
			} else {
				this.outstream.write(data, 0, size);
			}
			this.last_out = size;
			this.total_out += size;
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getMessage());
		}
	}

	public void writeLine(String data) {
		byte[] bytearray = (data + "\n").getBytes(StandardCharsets.UTF_8);
		this.write(bytearray, bytearray.length);
	}

	public String NioReadString() {
		int size;
		
		// byte[] vs ByteBuffer
        // https://stackoverflow.com/questions/5210840/when-to-use-byte-array-when-byte-buffer
		buffer.clear();
		try {
			size = this.socketChannel.read(this.buffer);
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getMessage());
			return null;
		}
		
		if (size == 0 ) {
			// no new data
			return "";
		} else if (size == -1) {
			// remote closed connection
            return null;
		}
		
		this.last_in = size;
		this.total_in += size;
		
		 MyLog.append("found " + size + " bytes in buffer");
		
        buffer.flip();
        // https://docs.oracle.com/javase/7/docs/api/java/nio/Buffer.html#flip()
        // Flips this buffer. The limit is set to the current position and then the
        // position is set to zero.
        // flips the buff after write to the buffer and before read from the buffer
        int n = buffer.limit();   
        
        if (n != size) {
            // this should never happen. just in case
        	MyLog.append(MyLog.ERROR, "n=" + n + " not equal to size=" + size);
            return null;
        }
        
		if (this.in_coder != null) {
			return (new String(this.in_coder.xor(buffer.array(), size), StandardCharsets.UTF_8));
		} else {
			return StandardCharsets.UTF_8.decode(buffer).toString();
		}		
	}

	public int streamReadBytes(byte[] buffer) {
		int size;
		
		// byte[] vs ByteBuffer
        // https://stackoverflow.com/questions/5210840/when-to-use-byte-array-when-byte-buffer
		try {
			size = this.socket.getInputStream().read(buffer); // this is blocked IO
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getMessage());
			return -1;
		}
		
		if (size == 0 ) {
			// no new data
			return 0;
		} else if (size == -1) {
			// remote closed connection
            return -1;
		}
		
		this.last_in = size;
		this.total_in += size;
		
        MyLog.append("received " + size + " bytes");
        
		if (this.in_coder != null) {
			this.in_coder.xorInplace(buffer, size);
		} 
		return size;
	}
	public void flush() {
		try {
			this.outstream.flush();
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getMessage());
		}
	}

	public void close () {
		try {
			this.outstream.close();
			this.socketChannel.close();
			this.socket.close();	
		} catch (IOException e) {
			MyLog.append(MyLog.ERROR, e.getMessage());
		}	
	}
}
