package com.tpsup.fileCopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class Server implements Runnable {
    private int port;
    private JTextArea log;
    private int wait_time_in_seconds;

    public Server(String port, JTextArea parentLog, int wait_time_in_seconds) {
        this.port = Integer.parseInt(port);
        this.log = parentLog;
        this.wait_time_in_seconds = wait_time_in_seconds;
    }

    static private final String newline = "\n";

    public void run() {
        ServerSocketChannel ssc;
        SocketChannel clientChannel;
        ServerSocket ss;
        try {
            // Instead of creating a ServerSocket, create a ServerSocketChannel
            ssc = ServerSocketChannel.open();
            // Get the Socket connected to this channel, and bind it to the listening port
            ss = ssc.socket();
            ss.setReuseAddress(true);
            InetSocketAddress isa = new InetSocketAddress(port);
            ss.bind(isa);
            ss.setSoTimeout(this.wait_time_in_seconds * 1000);
            log.append("bind port : " + port + newline);
        } catch (IOException ie) {
            log.append("Error : " + ie + newline);
            return;
        }
        try {
            clientChannel = ssc.accept(); // this thread is blocked here
            clientChannel.configureBlocking(false);
            log.append("Got connection from " + clientChannel.socket() + newline);
        } catch (IOException ie) {
            log.append("Error : " + ie + newline);
            try {
                ssc.close();
                ss.close();
            } catch (IOException e) {
                log.append("close socket failed : " + e);
            }
            return;
        }
        // close the listening port, as we only accept one connection
        try {
            ssc.close();
            ss.close();
        } catch (IOException e) {
            log.append("close socket failed : " + e);
        }
        HashMap<String, String> opt = new HashMap<>();
        opt.put("verbose", "0");
        String server_path;
        String client_block;
        int maxsize;
        String version;
        String exclude_string;
        String match_string;
        int deep_check;
        {
            String[] patternArray = { "<PATH>(.+)</PATH>", "<TREE>(.*)</TREE>", "<MAXSIZE>([-]?.+)</MAXSIZE>",
                    "<VERSION>(.+)</VERSION>", "<EXCLUDE>(.*)</EXCLUDE>", "<MATCH>(.*)</MATCH>",
                    "<DEEP>(.)</DEEP>" };
            ExpectSocket result = new ExpectSocket(clientChannel, patternArray, log, opt);
            if (!result.status.equals("matched")) {
                log.append(result.status);
                try {
                    clientChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            server_path = result.captures.get(0);
            client_block = result.captures.get(1);
            maxsize = Integer.parseInt(result.captures.get(2));
            version = result.captures.get(3);
            exclude_string = result.captures.get(4);
            match_string = result.captures.get(5);
            deep_check = Integer.parseInt(result.captures.get(6));
        }
        log.append("server_path = " + server_path + newline);
        if (!version.matches("^" + Version.mainVersion + ".*")) {
            log.append("client version '" + version + "' is not compatiable. Expecting " + Version.mainVersion
                    + ".x" + newline);
            return;
        }
        HashMap<String, HashMap> server_tree = DirTree.build_dir_tree(server_path, match_string,
                exclude_string);
        log.append("server_tree = " + server_tree + newline);
        HashMap<String, HashMap<String, String>> client_tree = new HashMap<>();
        if (client_block != null) {
            Pattern line_pattern = Pattern.compile("^key=");
            Pattern pair_pattern = Pattern.compile("^(.+?)=(.+)");
            for (String line : client_block.split("\n")) {
                if (line == null) {
                    continue;
                }
                Matcher line_matcher = line_pattern.matcher(line);
                if (!line_matcher.find()) {
                    continue;
                }
                HashMap<String, String> kv = new HashMap<String, String>();
                for (String pair : line.split("[|]")) {
                    Matcher pair_matcher = pair_pattern.matcher(pair);
                    if (pair_matcher.find()) {
                        kv.put(pair_matcher.group(1), pair_matcher.group(2));
                    }
                }
                client_tree.put(kv.get("key"), kv);
            }
        }
        log.append("client_tree = " + client_tree + newline);
        // compare server_tree with client_tree
        HashSet<String> back_exists_on_server = new HashSet<String>();
        HashMap<String, String> change_by_file = new HashMap<String, String>();
        HashSet<String> deletes = new HashSet<String>();
        // files to delete
        HashSet<String> modes = new HashSet<String>();
        // files needs mode resetting
        HashSet<String> diffables = new HashSet<String>();
        // files can be diff'ed
        HashSet<String> mtimes = new HashSet<String>();
        // files need mtime resetting
        HashSet<String> cksums = new HashSet<String>();
        // files need cksum calculating
        ArrayList<String> warns = new ArrayList<String>();
        // "back" is the relative root dir
        for (String k : server_tree.keySet()) {
            // k is a relative filename
            back_exists_on_server.add((String) server_tree.get(k).get("back"));
        }
        Pattern parent_dir_pattern = Pattern.compile("^(.+)/");
        for (String k : client_tree.keySet()) {
            // k is a relative filename
            if (!server_tree.containsKey(k)) {
                String client_back = (String) client_tree.get(k).get("back");
                // "back" is the relative root dir. only under the same "back"
                // (root) dir, we can compare client and server's path.
                if (back_exists_on_server.contains(client_back)) {
                    deletes.add(k);
                    // once we delete a file on client side, we need to sync up its parent dir's
                    // time stamp
                    Matcher parent_dir_matcher = parent_dir_pattern.matcher(k);
                    if (parent_dir_matcher.find()) {
                        String parent_dir = parent_dir_matcher.group(1);
                        if (server_tree.containsKey(parent_dir)) {
                            mtimes.add(parent_dir);
                        }
                    }
                } else {
                    log.append(k + "'s starting dir " + client_back
                            + " doesn't exist on server, don't delet it on client side " + newline);
                }
            }
        }
        // https://stackoverflow.com/questions/92252
        // 8/how-to-sort-map-values-by-key-in-java
        // https://stackoverflow.com/questions/35122490/how-to-reverse-the-order-of-sortedset
        SortedSet<String> reversedServerFileList = new TreeSet<String>(Collections.reverseOrder());
        reversedServerFileList.addAll(server_tree.keySet());
        long RequiredSpace = 0;
        for (String k : reversedServerFileList) {
            // k is a filename
            HashMap server_node = server_tree.get(k);
            long server_size = Integer.parseInt((String) server_node.get("size"));
            String server_type = (String) server_node.get("type");
            String server_mode = (String) server_node.get("mode");
            String server_mtime = (String) server_node.get("mtime");
            if (!client_tree.containsKey(k)) {
                // client is missing this file
                if (server_size > 0) {
                    if (maxsize >= 0 && RequiredSpace + server_size > maxsize) {
                        break;
                    }
                    RequiredSpace += server_size;
                }
                change_by_file.put(k, "add");
                Matcher parent_dir_matcher = parent_dir_pattern.matcher(k);
                if (parent_dir_matcher.find()) {
                    String parent_dir = parent_dir_matcher.group(1);
                    if (server_tree.containsKey(parent_dir)) {
                        mtimes.add(parent_dir);
                    }
                }
                if (server_type.equals("dir")) {
                    modes.add(k);
                }
                continue;
            }
            HashMap<String, String> client_node = client_tree.get(k);
            long client_size = Integer.parseInt(client_node.get("size"));
            String client_type = client_node.get("type");
            String client_mode = client_node.get("mode");
            String client_mtime = client_node.get("mtime");
            if (!client_type.equals(server_type)) {
                deletes.add(k);
                if (server_size > 0) {
                    if (maxsize >= 0 && RequiredSpace + server_size > maxsize) {
                        break;
                    }
                    RequiredSpace += server_size;
                }
                change_by_file.put(k, "newType");
                if (server_type.equals("dir")) {
                    // We don't tar dir because that would tar up all files under dir.
                    // But the problem with this approach is that the dir mode
                    // (permission) is then not recorded in the tar file. We will have
                    // to keep and send the mode information separately (from the tar file)
                    modes.add(k);
                }
                continue;
            }
            // now both sides are same kind type: file, dir, or link
            // we don't modify link's mode
            if (!server_type.equals("link") && !client_mode.equals(server_mode)) {
                modes.add(k);
            }
            if (client_size != server_size) {
                if (server_size > 0) {
                    if (maxsize >= 0 && RequiredSpace + server_size > maxsize) {
                        break;
                    }
                    RequiredSpace += server_size;
                }
                change_by_file.put(k, "update");
                diffables.add(k);
                continue;
            }
            // compare {test} if it is populated
            // dir's {test} and link's {test} are hardcoded, we are really only compare
            // files.
            if (!server_node.containsKey("test") && !client_node.containsKey("test")) {
                // if both missing tests, we compare mtime first
                // for fast check (default), if size and mtime match, then no need to update.
                // for deep check, or when mtime not matching (but size matching), resort to
                // cksum.
                if (!client_mtime.equals(server_mtime)) {
                    cksums.add(k);
                    mtimes.add(k);
                } else if (deep_check != 0) {
                    client_node.put("mtime", server_mtime);
                    cksums.add(k);
                }
            } else if (!server_node.containsKey("test") || !client_node.containsKey("test")) {
                // we reach here if only one test is missing.
                // note: if both tests missing, the logic above would take care of it.
                // not sure what situation will lead us here yet
                change_by_file.put(k, "update");
            } else if (!server_node.get("test").equals(client_node.get("test"))) {
                // now both tests exist, we can safely compare
                // not sure what situation will lead us here yet
                change_by_file.put("k", "update");
            } else {
                // server_node.get("test") == client_node.get("test")
                if (!client_mtime.equals(server_mtime)) {
                    mtimes.add(k);
                }
            }
        }
        OutputStreamWriter osw = null;
        try {
// turn on blocking before sending out data
            clientChannel.configureBlocking(true);
            osw = new OutputStreamWriter(clientChannel.socket().getOutputStream(), "UTF-8");
            String need_cksums_string = "<NEED_CKSUMS>" + String.join("\n", cksums) + "</NEED_CKSUMS>\n";
            osw.write(need_cksums_string, 0, need_cksums_string.length());
            log.append("sending need_chksums request to client: " + cksums.size() + " items" + newline);
            // need to flush blocked (buffered) connection
            osw.flush();
        } catch (IOException e) {
            log.append("failed to write to client" + newline);
            e.printStackTrace();
        }
        log.append("collecting server side cksums: " + newline);
        HashMap<String, String> server_cksum_by_file = Cksum
                .get_cksums(cksums.toArray(new String[cksums.size()]), server_tree);
        log.append("server_cksum_by_file : " + server_cksum_by_file + newline);
        String client_cksum_string;
        {
            log.append("waiting client cksum results: " + newline);
            String[] patternArray = { "<CKSUM_RESULTS>(.*)</CKSUM_RESULTS>" };
            ExpectSocket result = new ExpectSocket(clientChannel, patternArray, log, opt);
            if (!result.status.equals("matched")) {
                log.append(result.status + newline);
                return;
            }
            // Path server_path = Paths.get(result.captures.get(0));
            client_cksum_string = result.captures.get(0);
        }
        HashMap<String, String> client_cksum_by_file = new HashMap<String, String>();
        if (client_cksum_string != null && client_cksum_string.length() != 0) {
            Pattern pattern = Pattern.compile("^(.+?)[ ](.+)$");
            for (String line : client_cksum_string.split("\n")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String cksum = matcher.group(1);
                    String filename = matcher.group(2);
                    client_cksum_by_file.put(filename, cksum);
                } else {
                    log.append("bad format at line : '" + line + "'" + newline);
                }
            }
        }
        log.append("client_cksum_by_file = " + client_cksum_by_file + newline);
        log.append("cksums" + cksums + newline);
        log.append("mtimes" + mtimes + newline);
        log.append("deletes" + deletes + newline);
        log.append("modes" + modes + newline);
        log.append("change_by_file" + change_by_file + newline);
        SortedSet<String> adds = new TreeSet<String>();
        try {
            // block 10 before writing to client
            clientChannel.configureBlocking(true);
            String deletes_string = "<DELETES>" + String.join("\n", deletes) + "</DELETES>" + newline;
            osw.write(deletes_string, 0, deletes_string.length());
            log.append("sending deletes : " + deletes.size() + " items" + newline);
            adds.addAll(change_by_file.keySet());
            String adds_string = "<ADDS>";
            for (String f : adds) {
                // https://stackoverflow.com/questions/47045/sprintf-equivalent-in-java
                adds_string += String.format("%6s %s\n", change_by_file.get(f), f);
            }
            adds_string += "</ADDS>" + newline;
            osw.write(adds_string, 0, adds_string.length());
            log.append("sending adds : " + adds.size() + " items" + newline);
            String mtimes_string = "<MTIMES>";
            for (String f : mtimes) {
                log.append("mtimes " + f + " " + server_tree.get(f) + newline);
                mtimes_string += String.format("%s %s\n", server_tree.get(f).get("mtime"), f);
            }
            mtimes_string += "</MTIMES>" + newline;
            osw.write(mtimes_string, 0, mtimes_string.length());
            log.append("sending mtimes : " + mtimes.size() + " items" + newline);
            String modes_string = "<MODES>";
            for (String f : modes) {
                modes_string += String.format("%s %s\n", server_tree.get(f).get("mode"), f);
            }
            modes_string += "</MODES>" + newline;
            osw.write(modes_string, 0, modes_string.length());
            log.append("sending modees : " + modes.size() + " items" + newline);
            String warns_string = "<WARNS>" + String.join("\n", warns) + "</WARNS>" + newline;
            osw.write(warns_string, 0, warns_string.length());
            log.append("sending warns : " + warns.size() + " items" + newline);
            String space_string = "<SPACE>" + RequiredSpace + "</SPACE>" + newline;
            osw.write(space_string, 0, space_string.length());
            log.append("sending RequiredSpace : " + RequiredSpace + " bytes" + newline);
            // need to flush blocked (buffered) connection
            osw.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String mode;
        {
            log.append("waiting for transfer mode from client: " + newline);
            String[] patternArray = { "please send (data|diff|unpacked)" };
            ExpectSocket result = new ExpectSocket(clientChannel, patternArray, log, opt);
            if (!result.status.equals("matched")) {
                log.append(result.status + newline);
                return;
            }
            mode = result.captures.get(0);
        }
        log.append("received client tranfer mode: " + mode + ". creating local tar file" + newline);
        if (!mode.equals("data") && !mode.equals("diff")) {
            log.append("tranfer mode '" + mode + "' is not supported" + newline);
            return;
        }
        String tmpFile = TmpFile.createTmpFile(null, "filecopy_server", new HashMap<String, String>());
        log.append("tmpFile : " + tmpFile + newline);
        ArrayList<String> files_to_tar = new ArrayList<String>();
        if (mode.equals("diff")) {
            files_to_tar.addAll(diffables);
        } else {
            files_to_tar.addAll(adds);
        }
        if (files_to_tar.size() == 0) {
            log.append("no need to send anything to client" + newline);
            return;
        }
        HashMap<String, ArrayList<String>> files_by_front = new HashMap<>();
        for (String f : files_to_tar) {
            String front = (String) server_tree.get(f).get("front");
            if (server_tree.get(f).get("type").equals("dir")) {
                // Skip non-empty dir because tar'ing dir will also tar the files underneath.
                // But we include empty dir
                File dir = new File(front + '/' + f);
                if (dir.list().length != 0) {
                    continue;
                }
            }
            if (!files_by_front.containsKey(front)) {
                ArrayList<String> files = new ArrayList<String>();
                files_by_front.put(front, files);
            }
            files_by_front.get(front).add(f);
        }
        log.append("files_by_front = " + files_by_front + newline);
        try {
            TarArchiveOutputStream tarArchiveOutputStream = null;
            for (String front : files_by_front.keySet()) {
                tarArchiveOutputStream = Tar.createTar(tmpFile, front, files_by_front.get(front), true,
                        tarArchiveOutputStream);
                tarArchiveOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // turn on blocking before sending out data
        int total_size = 0;
        try {
            clientChannel.configureBlocking(true);
            // https://docs.oracle.com/javase/6/docs/api/java/net/Socket.html#getOutputStream()
            // https://stackoverflow.com/questions/9046820/fastest-way-to-incrementally-read-a-large-file
            InputStream inputStream = new FileInputStream(tmpFile);
            OutputStream outputStream = clientChannel.socket().getOutputStream();
            // https://stackoverflow.com/questions/9046820/fastest-way-to-incrementally-read-a-large-file
            byte buffer[] = new byte[1024 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                log.append("bytes read and sent = " + read + newline);
                outputStream.write(buffer, 0, read);
                total_size += read;
            }
            outputStream.flush();
            inputStream.close();
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.append("sent total_size=" + total_size + ". closed client connection\n");
        log.append(" \n\n");
        return;
    }
}
