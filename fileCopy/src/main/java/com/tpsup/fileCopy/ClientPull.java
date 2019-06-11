package com.tpsup.fileCopy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import javax.swing.JTextArea;

public class ClientPull implements Runnable {
    private String remoteHost;
    private int remotePort;
    private String remotePath;
    private String localPath;
    private String match_string = "";
    private String exclude_string = "";
    private boolean dryrun = false;
    private JTextArea log;

    public ClientPull(String remoteHost, String remotePort, String remotePath, String localPath,
            JTextArea parentLog, HashMap<String, String> opt) {
        this.remoteHost = remoteHost;
        this.remotePort = Integer.parseInt(remotePort);
        this.remotePath = remotePath;
        this.localPath = localPath;
        this.log = parentLog;
        if (opt.containsKey("MatchString")) {
            this.match_string = opt.get("MatchString");
        }
        if (opt.containsKey("ExcludeString")) {
            this.exclude_string = opt.get("ExcludeString");
        }
        if (opt.containsKey("timeout")) {
            Integer.parseInt(opt.get("timeout"));
        }
        if (opt.containsKey("dryrun") && opt.get("dryrun").equals("1")) {
            this.dryrun = true;
        }
    }

    static private final String newline = "\n";

    public void run() {
        this.localPath.replaceAll("/+$", ""); // remove ending /
        if (this.remotePath.matches("^/+$")) {
            log.append("cannot handle remotePath = " + this.remotePath + newline);
            return;
        }
        this.remotePath.replace("/+$", ""); // remote the trailing /
        String back;
        {
            String[] array = this.remotePath.split("/+");
            back = array[array.length - 1];
        }
        String target_path = localPath + "/" + back;
        ArrayList<String> target_paths = new ArrayList<String>();
        {
// https://docs.oracle.com/javase/tutorial/essential/io/find.html
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + target_path);
            File[] list = (new File(localPath)).listFiles();
            if (list != null) {
                for (File f : list) {
                    if (matcher.matches(f.toPath())) {
                        target_paths.add(f.toString());
                    }
                }
            }
        }
        long maxsize = -1;
        HashMap<String, HashMap> client_tree = DirTree.build_dir_tree(target_paths, this.match_string,
                this.exclude_string);
        log.append("client_tree = " + client_tree + newline);
        SocketChannel sc = null;
        Socket s = null;
        OutputStreamWriter osw = null;
        int max_try = 5;
        for (int i = 1; i <= max_try; i++) {
            String error = "";
            boolean connected = true;
            try {
                sc = SocketChannel.open();
                s = sc.socket();
                s.connect(new InetSocketAddress(remoteHost, remotePort), 3000);
                osw = new OutputStreamWriter(s.getOutputStream(), "UTF-8");
            } catch (IOException el) {
                el.printStackTrace();
                error = el.getMessage();
                connected = false;
            }
            if (connected) {
                break;
            } else {
                log.append(
                        "remote server " + remoteHost + ":" + remotePort + " is not ready: " + error + "\n");
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (i < max_try) {
                    int seconds = 5;
                    log.append("Will retry in " + seconds + " seconds for " + (max_try - i) + " time(s)\n");
                    try {
                        Thread.sleep(seconds * 1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.append("no more retry\n");
                }
            }
        }
        try {
// buffer before write to server
            sc.configureBlocking(true);
            String version_string = "<VERSION>" + Version.version + "</VERSION>\n";
            log.append("sending version: " + Version.version + newline);
            osw.write(version_string, 0, version_string.length());
            String path_string = "<PATH>" + this.remotePath + "</PATH>\n";
            log.append("sending path: " + this.remotePath + newline);
            osw.write(path_string, 0, path_string.length());
            String deep_string = "<DEEP>" + "0" + "</DEEP>\n";
            log.append("sending deep check flag: " + 0 + newline);
            osw.write(deep_string, 0, deep_string.length());
            StringBuilder bld = new StringBuilder();
            bld.append("<TREE>\n");
            for (String f : client_tree.keySet()) {
                ArrayList<String> list = new ArrayList<String>();
                list.add("key=" + f);
                for (String attr : ((HashMap<String, String>) client_tree.get(f)).keySet()) {
                    String string = attr + "=" + client_tree.get(f).get(attr);
                    list.add(string);
                }
                String line = StringUtils.join(list, "|");
                bld.append(line);
                bld.append("\n");
            }
            bld.append("</TREE>\n");
            String client_tree_string = bld.toString();
            log.append("sending client_tree: " + client_tree.size() + " items" + newline);
            osw.write(client_tree_string, 0, client_tree_string.length());
            String maxsize_string = "<MAXSIZE>" + maxsize + "</MAXSIZE>\n";
            log.append("sending maxsize: " + maxsize + newline);
            osw.write(maxsize_string, 0, maxsize_string.length());
            String excludes_string = "<EXCLUDE>" + exclude_string + "</EXCLUDE>\n";
            log.append("sending excludes_string: " + excludes_string + newline);
            osw.write(excludes_string, 0, excludes_string.length());
            String matches_string = "<MATCH>" + match_string + "</MATCH>\n";
            log.append("sending matches_string: " + matches_string + newline);
            osw.write(matches_string, 0, matches_string.length());
            osw.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        log.append("waiting cksum requests from server ..." + newline);
        String need_cksums_string;
        {
            String[] patternArray = { "<NEED_CKSUMS>(.*)</NEED_CKSUMS>" };
            HashMap<String, String> opt = new HashMap<String, String>();
            ExpectSocket result = new ExpectSocket(sc, patternArray, log, opt);
            if (!result.status.equals("matched")) {
                log.append(result.status + newline);
                log.append("we closed connection" + newline);
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            need_cksums_string = result.captures.get(0);
        }
        String[] need_cksums = need_cksums_string.split("\n");
        log.append("received cksum requests, calculating cksums" + newline);
        HashMap<String, String> client_cksums_by_file = Cksum.get_cksums(need_cksums, client_tree);
        String cksums_results_string = null;
        {
            StringBuilder bld = new StringBuilder();
            bld.append("<CKSUM_RESULTS>\n");
            for (String file : client_cksums_by_file.keySet()) {
                bld.append(client_cksums_by_file.get(file) + " " + file + "\n");
            }
            bld.append("</CKSUM_RESULTS>\n");
            cksums_results_string = bld.toString();
        }
        // buffer before write to server
        try {
            sc.configureBlocking(true);
            log.append("sending cksums results: " + client_cksums_by_file.size() + " items" + newline);
            osw.write(cksums_results_string, 0, cksums_results_string.length());
            osw.flush();
        } catch (IOException el) {
            el.printStackTrace();
        }
        log.append("waiting instructions from server..." + newline);
        String deletes_string;
        String mtimes_string;
        String adds_string;
        String warns_string;
        String RequiredSpace_string;
        {
            String[] patternArray = { "<DELETES>(.*)</DELETES>", "<MTIMES>(.*)</MTIMES>",
                    "<MODES>(.*)</MODES>", "<SPACE>(\\d+)</SPACE>", "<ADDS>(.*)</ADDS>",
                    "<WARNS>(.*)</WARNS>" };
            HashMap<String, String> opt = new HashMap<>();
            ExpectSocket result = new ExpectSocket(sc, patternArray, log, opt);
            if (!result.status.equals("matched")) {
                log.append(result.status + newline);
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.append("we closed connection" + newline);
                return;
            }
            deletes_string = result.captures.get(0);
            mtimes_string = result.captures.get(1);
            result.captures.get(2);
            RequiredSpace_string = result.captures.get(3);
            adds_string = result.captures.get(4);
            warns_string = result.captures.get(5);
        }
        if (!deletes_string.isEmpty()) {
            String[] deletes = deletes_string.split("\n");
            String last_delete = "";
            for (String d : deletes) {
                if (last_delete.isEmpty() || !d.matches("^" + last_delete + "/.*")) {
                    // if we already deleted the dir, no need to delete files under it.
                    log.append("rm -fr " + d + newline);
                    String front = (String) client_tree.get(d).get("front");
                    try {
                        String localPath = front + "/" + d;
                        File f = new File(localPath);
                        if (!f.exists()) {
                            log.append(f + " does't exist locally" + newline);
                        } else {
                            if (f.isDirectory()) {
                                FileUtils.deleteDirectory(f);
                            } else {
                                FileUtils.forceDelete(f);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    last_delete = d;
                }
            }
        }
        if (!adds_string.isEmpty()) {
            HashMap<String, String> action_by_file = new HashMap<String, String>();
            String[] adds = adds_string.split("\n");
            Pattern pair_pattern = Pattern.compile("^[ ]*([^ ]+)[ ](.+)");
            for (String a : adds) {
                String action;
                String file;
                Matcher pair_matcher = pair_pattern.matcher(a);
                if (pair_matcher.find()) {
                    action = pair_matcher.group(1);
                    file = pair_matcher.group(2);
                    action_by_file.put(file, action);
                } else {
                    log.append("unexpected format " + a + ". expecting: action file" + newline);
                }
            }
            for (String f : action_by_file.keySet()) {
                log.append(String.format("%10s %s\n", action_by_file.get(f), f));
            }
        }
        if (!warns_string.isEmpty()) {
            String[] warns = warns_string.split("\n");
            for (String w : warns) {
                log.append("warning from server side: " + w + newline);
            }
        }
        if (adds_string.isEmpty()) {
            log.append("nothing to add or update\n");
        } else if (!dryrun) {
            String tmpFile;
            {
                HashMap<String, String> opt = new HashMap<>();
                opt.put("chkSpace", RequiredSpace_string);
                tmpFile = TmpFile.createTmpFile(null, "filecopy_client", opt);
            }
            if (tmpFile == null) {
                log.append("failed to create tmpFile" + newline);
                return;
            }
            log.append("tmpFile = " + tmpFile + newline);
            try {
// buffer before write to server
                sc.configureBlocking(true);
                log.append("asking for data\n");
                osw.write("please send data");
                osw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
// turn on blocking before sending out data
            int total_size = 0;
            try {
                sc.configureBlocking(true);
                OutputStream outputStream = new FileOutputStream(tmpFile);
                InputStream inputStream = sc.socket().getInputStream();
// https://stackoverflow.com/questions/9046820/fastest-way-to-incrementally-read-a-large-fi
                byte buffer[] = new byte[1024 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    log.append("bytes read and sent = " + read + newline);
                    outputStream.write(buffer, 0, read);
                    total_size += read;
                }
                outputStream.flush();
                outputStream.close();
                log.append("received total_size=" + total_size + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Tar.unTar(tmpFile, localPath);
            } catch (Exception e1) {
                e1.printStackTrace();
                log.append("unTar(" + tmpFile + ", " + localPath + ") failed\n");
                return;
            }
            log.append("extracted tar files from " + tmpFile + " to " + localPath + "\n");
        }
        try {
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.append("closed connect ion\n");
        if (!mtimes_string.isEmpty()) {
            Pattern pattern = Pattern.compile("^(\\d+?)[ ](.+)$");
            for (String line : mtimes_string.split("\n")) {
                if (line.isEmpty()) {
                    continue;
                }
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String mtime = matcher.group(1);
                    String filename = matcher.group(2);
                    FileTime fileTime = FileTime.fromMillis((long) (Integer.parseInt(mtime)) * 1000);
                    String fullName = localPath + "/" + filename;
                    BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(fullName),
                            BasicFileAttributeView.class);
                    log.append("set mtime: " + mtime + " " + filename + "\n");
                    try {
                        attributes.setTimes(fileTime, fileTime, fileTime);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    log.append("bad format at line : '" + line + "'" + newline);
                }
            }
        }
        log.append("all done\n	\n\n");
    }

    HashMap<String, String> get_cksums(HashSet<String> files) {
        HashMap<String, String> cksum_by_file = new HashMap<String, String>();
        Iterator<String> iter = files.iterator();
        while (iter.hasNext()) {
            String file = iter.next();
            String cksum = "";
            try {
                cksum = String.valueOf(Cksum.checksumMappedFile(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            cksum_by_file.put(file, cksum);
        }
        return cksum_by_file;
    }
}
