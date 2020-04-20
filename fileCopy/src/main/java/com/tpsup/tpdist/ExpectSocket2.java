package com.tpsup.tpdist;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;

public class ExpectSocket2 {
    public String status = "10 problem";
    public ArrayList<String> captures = null;
    private String newline = "\n";

    // total_string = <VERSION>6.3</VERSION>
    // <PATH>C:/Users/Public/Documents/CYGWIN/home/hantian/testdir
    // C:/Users/Public/Documents/CYGWIN/home/hantian/testdir2</PATH><DEEP>0</DEEP><TREE>
    // key=testdir2|back=testdir2|front=/home/axptsusu/|mode=0755|mtime=1546436740|size=128|test=dir|type=dir
    // key=testdir2/e.txt|back=testdir2|front=/home/axptsusu/|mode=0644|mtime=1546436740|size=5|type=file
    // </TREE><MAXSIZE>-l</MAXSIZE><EXCLUDE></EXCLUDE><MATCH></MATCH>
    public ExpectSocket2(SocketChannel sc, String[] patternArray, JTextArea log, HashMap<String, String> opt) {
        // turn off blocking (buffer) before reading out data
        try {
            sc.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            this.status = e.getClass().getName();
            return;
        }
        StringBuilder bld = new StringBuilder();
        int receive_timeout = 10; // seconds
        int receive_time_so_far = 0; // seconds;
        ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
        while (true) {
            try {
                Thread.sleep(1000); // 1000 milliseconds is one second.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            receive_time_so_far++;
            if (receive_time_so_far > receive_timeout) {
                this.status = "failed to get expected data within " + receive_time_so_far + " seconds > "
                        + receive_timeout + " seconds";
                return;
            }
            buffer.clear();
            int size;
            try {
                size = sc.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                this.status = e.getClass().getName();
                return;
            }
            if (size == -1) {
                this.status = "counterparty closed connection";
                return;
            } else if (size == 0) {
                continue; // no new data
            }
            buffer.flip();
            // https://docs.oracle.com/javase/7/docs/api/java/nio/Buffer.html#flip()
            // Flips this buffer. The limit is set to the current position and then the
            // position is set to zero.
            // flips the buff after write to the buffer and before read from the buffer
            int n = buffer.limit();
            log.append("received " + n + " bytes" + newline);
            bld.append(StandardCharsets.UTF_8.decode(buffer).toString());
            log.append("total_string = " + bld.toString() + newline);
            // https://www.geeksforgeeks.org/array-vs-arraylist-in-java/
            ArrayList<String> captures = new ArrayList<String>();
            Boolean all_matched = true;
            for (String patternString : patternArray) {
                // https://stackoverflow.com/questions/17969436/java-regex-capturing-groups
                Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);
                Matcher matcher = pattern.matcher(bld.toString());
                log.append("testing " + patternString + newline);
                if (matcher.find()) {
                    captures.add(matcher.group(1));
                    log.append("matched" + newline);
                } else {
                    all_matched = false;
                }
            }
            if (all_matched) {
                log.append("we got all expected data, returning " + newline);
                this.status = "matched";
                this.captures = captures;
                return;
            } else {
                log.append("we haven't got all expected data, entering next round" + newline);
                continue;
            }
        }
    }
}
