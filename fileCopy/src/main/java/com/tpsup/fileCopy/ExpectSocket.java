package com.tpsup.fileCopy;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;

public class ExpectSocket {
    private MyConn myconn = null;
    private String newline = "\n";
    private ByteBuffer buffer = null;
    private Socket socket = null;
    private SocketChannel socketChannel = null;
    private JTextArea log = null;
    int ExpectTimeout = -1; // seconds

    public ExpectSocket(MyConn myconn, JTextArea log, HashMap<String, Object> opt) {
        this.myconn = myconn;
        this.socket = myconn.socket;
        this.socketChannel = myconn.socketChannel;
        this.buffer = ByteBuffer.allocate( 1024*1024);
        this.log = log;
        
        if (opt.containsKey("ExpectTimeout")) {
            this.ExpectTimeout = (int) opt.get("ExpectTimeout");            
        }
    }
        
    // total_string = <VERSION>6.3</VERSION>
    // <PATH>C:/Users/Public/Documents/CYGWIN/home/hantian/testdir
    // C:/Users/Public/Documents/CYGWIN/home/hantian/testdir2</PATH><DEEP>0</DEEP><TREE>
    // key=testdir2|back=testdir2|front=/home/axptsusu/|mode=0755|mtime=1546436740|size=128|test=dir|type=dir
    // key=testdir2/e.txt|back=testdir2|front=/home/axptsusu/|mode=0644|mtime=1546436740|size=5|type=file
    // </TREE><MAXSIZE>-l</MAXSIZE><EXCLUDE></EXCLUDE><MATCH></MATCH>
    public ArrayList<ArrayList<String>> capture(String[] patterns, HashMap<String, Object> opt) {
        ArrayList<ArrayList<String>> captures = new ArrayList<ArrayList<String>>();
        StringBuilder bld = new StringBuilder();
        int total_wait = 0; // seconds
        int this_section_recv = 0; //number of bytes
        String error_message = null;
        
        // https://stackoverflow.com/questions/12756360/how-to-make-java-set/12756499
        Set<Integer> matched = new HashSet<Integer>();
        
        int ExpectTimeout = this.ExpectTimeout;
        if (opt.containsKey("ExpectTimeout")) {
            ExpectTimeout = (int) opt.get("ExpectTimeout");
        }
        
        ArrayList<Pattern> compiled_patterns = new ArrayList<Pattern>();
        for (String pattern : patterns) {
            Pattern compiled_pattern = Pattern.compile(pattern, Pattern.DOTALL);
            compiled_patterns.add(compiled_pattern);
        }
        
        try {
            // turn off blocking (buffer) before reading out data
            this.socketChannel.configureBlocking(false);
            
            while (true) {                
                buffer.clear();
                int size = this.socketChannel.read(buffer);               
                if (size == 0) {
                    // no new data
                    if (total_wait > ExpectTimeout) {
                        error_message = "timed out after " + ExpectTimeout
                                + " seconds. very likely wrong protocol. expecting "
                                + Version.expected_protocol + ".*";
                        myconn.writer.write(error_message);
                        break;
                    }                   
                    Thread.sleep(1000); // 1000 milliseconds is one second.
                    total_wait ++;
                    continue;
                } else if (size == -1) {
                    error_message = "remote side closed socket";
                    break;
                }
                
                myconn.in_count += size;
                this_section_recv += size;
                
                buffer.flip();
                // https://docs.oracle.com/javase/7/docs/api/java/nio/Buffer.html#flip()
                // Flips this buffer. The limit is set to the current position and then the
                // position is set to zero.
                // flips the buff after write to the buffer and before read from the buffer
                int n = buffer.limit();
               
                log.append("found " + size + " bytes in buffer" + newline);
                
                if (n != size) {
                    // this should never happen. just in case
                    error_message = "n=" + n + " not equal to size=" + size + newline;
                    break;
                }
                bld.append(StandardCharsets.UTF_8.decode(buffer).toString());
                log.append("received " + size + " byte(s), total=" + myconn.in_count + 
                        ", this section so far "  + this_section_recv + " byte(s)"
                        + bld.toString() + newline);
                
                // https://www.geeksforgeeks.org/array-vs-arraylist-in-java/
                // ArrayList<String> captures = new ArrayList<String>();
                Boolean all_matched = true;
                for (int i = 0; i < compiled_patterns.size(); i++) {
                    if (matched.contains(i)) {
                        continue;
                    }
                    // https://stackoverflow.com/questions/17969436/java-regex-capturing-groups
                    Pattern pattern = compiled_patterns.get(i);
                    Matcher matcher = pattern.matcher(bld.toString());
                    
                    if (matcher.find()) {
                        captures.add(i, new ArrayList<String>());
                        captures.get(i).add(matcher.group(1));
                        captures.get(i).add(matcher.group(2));
                        captures.get(i).add(matcher.group(3));
                        log.append("matched pattern= " + patterns[i] + newline);
                        matched.add(i);
                    } else {
                        all_matched = false;
                    }
                }
                if (all_matched) {
                    log.append("received complete information from remote" + newline);
                    return captures;
                }               
            }
        } catch (Exception e) {
            error_message = e.getStackTrace().toString();            
        }
        
        if (error_message != null) {
            log.append(error_message + newline);
            log.append("matched so far");
            for (int i=0; i<patterns.length; i++) {
                String line = "   pattern=" + patterns[i] + "  ";
                if (matched.contains(i)) {
                    line += "matched" + newline;
                } else {
                    line += "not matched" + newline;
                }
                log.append(line);
            }
            
            String data = bld.toString();
            int data_size = data.length();
            int print_size = data_size;
            if (print_size > 100) {
                log.append("   Last words: " + data.substring(data_size - 100) + newline);
            } else {
                log.append("   Last words: " + data + newline);
            }
            
            
            }
        }
        
        return null;
    }
}
