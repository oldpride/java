package com.tpsup.tpdist;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import javax.swing.JTextArea;

public class ClientPush implements Runnable {
    static private final String newline = "\n";
    private String inputFilePath;
    private String Url;
    private JTextArea log;

    public ClientPush(String inputFilePath, String Url, JTextArea log) {
        this.inputFilePath = inputFilePath;
        this.Url = Url;
        this.log = log;
    }

    public void run() {
        String[] parts = Url.split(":");
        String tmpFile = TmpFile.createTmpFile(null, "filecopy_push", new HashMap<String, String>());
        log.append("tmpFile : " + tmpFile + ". creating tar file" + newline);
        try {
            Tar.createTar(tmpFile, inputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            log.append("failed to tar " + inputFilePath + newline);
            return;
        }
        String ServerHost = parts[0];
        int ServerPort = Integer.parseInt(parts[1]);
        Socket socket = null;
        try {
            socket = new Socket(ServerHost, ServerPort);
        } catch (IOException e) {
            e.printStackTrace();
            log.append("failed to connect to " + this.Url + "\n");
            return;
        }
        File tmpFileFile = new File(tmpFile);
        // https://www.codejava.net/java-se/file-io/how-to-read-and-write-binary-files-in-java
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(tmpFileFile);
            // use Buffer for performance
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            OutputStream outputStream = socket.getOutputStream();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            int byteRead;
            while ((byteRead = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(byteRead);
            }
            inputStream.close();
            outputStream.close();
            socket.close();
            log.append("pushed to " + Url + newline);
        } catch (IOException e) {
            e.printStackTrace();
            log.append("failed to open FileInputStream for " + tmpFile + newline);
        }
    }
}
