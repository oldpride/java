package com.tpsup.tpdist;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

public class FileCopyGui extends JPanel implements ActionListener {
    static private final String newline = "\n";
    JButton listenButton;
    JTextArea log;
    // server
    JTextField portText;
    // client pull
    JTextField serverText, remotePathText, localPathText, excludeText, matchText;
    JButton pullButton, pullDryrunButton;
    // client push
    JTextField localPushPathText, pushServerText;
    JButton browseButton, pushButton;
    JFileChooser fc;

    public FileCopyGui() {
        super(new BorderLayout());
        // Create the log first, because the action listeners need to refer to it.
        log = new JTextArea(30, 60);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        portText = new JTextField();
        // server part
        JLabel serverRoleLabel = new JLabel("As a Server:");
        JLabel portLabel = new JLabel("Local Listening Port");
        portText.setText("5555");
        listenButton = new JButton("Server start to Listen!");
        listenButton.addActionListener(this);
        // As a client to pull
        JLabel clientPullRoleLabel = new JLabel("As a Client to Pull:");
        serverText = new JTextField();
        JLabel serverLabel = new JLabel("Remote Server Host:Port");
        serverText.setText("mypc.abc.com:5555");
        remotePathText = new JTextField();
        JLabel remotePathLabel = new JLabel("Remote Path");
        remotePathText.setText("C:/Users/william/testdir");
        localPathText = new JTextField();
        JLabel localPathLabel = new JLabel("Local Path");
        localPathText.setText("C:/Users/william/client");
        excludeText = new JTextField();
        JLabel excludeLabel = new JLabel("RegEx Exclude Pattern (optional)");
        excludeText.setText("");
        matchText = new JTextField();
        JLabel matchLabel = new JLabel("RegEx Match Pattern (optional)");
        matchText.setText("");
        pullDryrunButton = new JButton("Client Pull Dryrun");
        pullDryrunButton.addActionListener(this);
        pullButton = new JButton("Client Pulll");
        pullButton.addActionListener(this);
        // As a client to push
        JLabel clientPushRoleLabel = new JLabel("As a Client to Push:");
        pushServerText = new JTextField();
        JLabel pushServerLabel = new JLabel("Server Host:Port");
        pushServerText.setText("remotehost.abc.com:5555");
        localPushPathText = new JTextField();
        JLabel localPushPathLabel = new JLabel("Path");
        localPushPathText.setText("C:/Users/william/testdir");
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        browseButton = new JButton("or Browse ...");
        browseButton.addActionListener(this);
        pushButton = new JButton("Push!");
        pushButton.addActionListener(this);
        // For layout purposes, put the buttons in a separate panel
        JPanel pane = new JPanel(new GridBagLayout());
        // use FlowLayout
        int row = 0;
        // Server
        pane.add(serverRoleLabel, gbc(row, 0, 1, false));
        row++;
        pane.add(portLabel, gbc(row, 0, 1, false));
        pane.add(portText, gbc(row, 1, 1, true));
        row++;
        pane.add(listenButton, gbc(row, 2, 1, false));
        // As a client to pull
        row++;
        pane.add(clientPullRoleLabel, gbc(row, 0, 1, false));
        row++;
        pane.add(serverLabel, gbc(row, 0, 1, false));
        pane.add(serverText, gbc(row, 1, 1, true));
        row++;
        pane.add(remotePathLabel, gbc(row, 0, 1, false));
        pane.add(remotePathText, gbc(row, 1, 1, true));
        row++;
        pane.add(localPathLabel, gbc(row, 0, 1, false));
        pane.add(localPathText, gbc(row, 1, 1, true));
        row++;
        pane.add(excludeLabel, gbc(row, 0, 1, false));
        pane.add(excludeText, gbc(row, 1, 1, true));
        row++;
        pane.add(matchLabel, gbc(row, 0, 1, false));
        pane.add(matchText, gbc(row, 1, 1, true));
        row++;
        pane.add(pullDryrunButton, gbc(row, 2, 1, false));
        pane.add(pullButton, gbc(row, 3, 1, false));
        // As a client to push
        row++;
        pane.add(clientPushRoleLabel, gbc(row, 0, 1, false));
        row++;
        pane.add(pushServerLabel, gbc(row, 0, 1, false));
        pane.add(pushServerText, gbc(row, 1, 1, true));
        row++;
        pane.add(localPushPathLabel, gbc(row, 0, 1, false));
        pane.add(localPushPathText, gbc(row, 1, 1, true));
        pane.add(browseButton, gbc(row, 2, 1, false));
        row++;
        pane.add(pushButton, gbc(row, 2, 1, false));
        // Add the buttons and the log to this panel,
        add(pane, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == pullButton || e.getSource() == pullDryrunButton) {
            if (alreadyHaveAChild()) {
                return;
            }
            String serverString = FileCopyGui.this.serverText.getText();
            String remotePath = FileCopyGui.this.remotePathText.getText();
            String localPath = FileCopyGui.this.localPathText.getText();
            String excludePattern = FileCopyGui.this.excludeText.getText();
            String matchPattern = FileCopyGui.this.matchText.getText();
            Pattern expectedServerPattern = Pattern.compile("^([^:]+):(\\d+)$");
            Matcher serverMatcher = expectedServerPattern.matcher(serverString);
            String remoteHost, remotePort;
            if (serverMatcher.find()) {
                remoteHost = serverMatcher.group(1);
                remotePort = serverMatcher.group(2);
            } else {
                log.append("remote server " + serverString
                        + "in bad format, shoud be Host:Port. Host can be hostname or IP, and Port must be an integer"
                        + newline);
                return;
            }
            log.append("spawning a client thread to connect to " + serverString + newline);
            HashMap<String, String> opt = new HashMap<>();
            opt.put("ExcludeString", excludePattern);
            opt.put("MatchString", matchPattern);
            opt.put("timeout", "5");
            if (e.getSource() == pullDryrunButton) {
                opt.put("dryrun", "1");
            }
            ClientPull clientPull = new ClientPull(remoteHost, remotePort, remotePath, localPath, log, opt);
            Thread thread = new Thread(clientPull);
            thread.setName("port-" + remotePort);
            thread.start();
        } else if (e.getSource() == listenButton) {
            if (alreadyHaveAChild()) {
                return;
            }
            String localPort = FileCopyGui.this.portText.getText();
            Pattern expectedPortPattern = Pattern.compile("^\\d+$");
            Matcher portMatcher = expectedPortPattern.matcher(localPort);
            if (!portMatcher.find()) {
                log.append("port " + localPort + "in bad format, should be an positive integer" + newline);
                return;
            }
            log.append("spawning a thread at port " + localPort + newline);
            Server listener = new Server(localPort, log, 5);
            Thread thread = new Thread(listener);
            thread.setName("port-" + localPort);
            thread.start();
        } else if (e.getSource() == browseButton) {
            int returnVal = fc.showDialog(this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                // This is where a real application would save the file, 
                log.append("selected: " + fc.getCurrentDirectory() + "/" + file.getName() + "." + newline);
                this.localPushPathText.setText(fc.getCurrentDirectory() + "/" + file.getName());
            } else {
                log.append("browser is cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == pushButton) {
            if (alreadyHaveAChild()) {
                return;
            }
            String SourcePath = this.localPushPathText.getText();
            String ServerUrl = this.pushServerText.getText();
            Pattern expectedPattern = Pattern.compile("^\\S+:\\S+$");
            Matcher matcher = expectedPattern.matcher(ServerUrl);
            if (matcher.find()) {
            } else {
                log.append("Server Host:Port= " + ServerUrl + "in bad format." + newline);
                return;
            }
            log.append("spawning a client thread to connect to " + ServerUrl + newline);
            ClientPush clientPush = new ClientPush(SourcePath, ServerUrl, log);
            Thread thread = new Thread(clientPush);
            thread.setName("port-" + ServerUrl);
            thread.start();
        }
    }

    private boolean alreadyHaveAChild() {
        Pattern threadPattern = Pattern.compile("port-");
        // https://stackoverflow.com/questions/15370120/get-thread-by-name
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (threadPattern.matcher(t.getName()).find()) {
                log.append("thread " + t.getName() + " is still running, cannot spawn new thread" + newline);
                return true;
            }
        }
        return false;
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = FileCopyGui.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked
     * from the event dispatch thread.
     */
    private static void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("FileCopy GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Add content to the window.
        frame.add(new FileCopyGui());
        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static Object gbc(int row, int column, int span, boolean resizeRight) {
        return new GridBagConstraints(column, row, span, 1, resizeRight ? 1. : 0., 0.,
                GridBagConstraints.WEST,
                resizeRight ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE, new Insets(5, 5, 5, 5),
                0, 0);
    }

    public static void main(String[] args) {
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Turn off metal's use of bold fonts 
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}
