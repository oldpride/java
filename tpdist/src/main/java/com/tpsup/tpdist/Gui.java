package com.tpsup.tpdist;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

public class Gui extends JPanel implements ActionListener {
	//https://www.baeldung.com/java-serial-version-uid
	private static final long serialVersionUID = 1L;
    
	// log area
    JTextArea logArea;
    // server
    JTextField serverPortText;
    JButton serverListenButton;
    // client
    JTextField clientHostPortText;
    // pull
    JTextField  pullRemotePathsText, pullLocalDirText, excludesText, matchesText;
    JButton pullButton, pullDryrunButton, pullDiffButton, browseButton;
    // to-be-pulled
    JButton bePulledButton;
    // encode 
    JTextField keyText;
   
    JFileChooser fileChooser;

    public Gui() {
        super(new BorderLayout());
        // Create the log first, because the action listeners need to refer to it.
        JTextArea logArea = new JTextArea(30, 60);
        logArea.setMargin(new Insets(5, 5, 5, 5));
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        MyLog.jtextarea = logArea; 
       
        // server
        JLabel serverRoleLabel = new JLabel("Run as a Server:");
        JLabel serverPortLabel = new JLabel("Local Listening Port");
        serverPortText = new JTextField();
        serverPortText.setText("5555");
        serverListenButton = new JButton("Start to listen!");
        serverListenButton.addActionListener(this);
        
        // client
        JLabel clientRoleLabel = new JLabel("Run as a Client to Pull:");
        JLabel clientHostPortLabel = new JLabel("Remote Server Host:Port");        
        clientHostPortText = new JTextField();        
        clientHostPortText.setText("localhost:5555");
        
        // pull
        JLabel toPullLabel = new JLabel("To pull:");
        
        JLabel pullRemotePathsLabel = new JLabel("Remote Paths, separated by |");
        pullRemotePathsText = new JTextField();
        pullRemotePathsText.setText("C:/Users/william/testdir"); 
        
        JLabel pullLocalDirLabel = new JLabel("Local Dir");       
        pullLocalDirText = new JTextField();
        pullLocalDirText.setText("C:/Users/william/client"); 
        
        JLabel excludesLabel = new JLabel("RegEx Exclude Patterns (optional, separated by ,)");
        excludesText = new JTextField();
        excludesText.setText("");
        
        JLabel matchesLabel = new JLabel("RegEx Match Pattern (optional, separated by ,)");
        matchesText = new JTextField();
        matchesText.setText("");
        
        pullDryrunButton = new JButton("Dryrun");
        pullDryrunButton.addActionListener(this);
        
        pullDiffButton = new JButton("Diff (will change nothing)");
        pullDiffButton.addActionListener(this);
        
        pullButton = new JButton("Pull");
        pullButton.addActionListener(this);
        
        // be pulled
        JLabel toBePulledLabel = new JLabel("To be pulled:");
        bePulledButton = new JButton("Be pulled");
        bePulledButton.addActionListener(this);
        
        // For layout purposes, put the buttons in a separate panel
        JPanel pane = new JPanel(new GridBagLayout());
        // use FlowLayout
        int row = 0;
        
        // server
        pane.add(serverRoleLabel, gbc(row, 0, 1, false));
        row++;
        pane.add(serverPortLabel, gbc(row, 0, 1, false));
        pane.add(serverPortText, gbc(row, 1, 1, true));
        row++;
        pane.add(serverListenButton, gbc(row, 2, 1, false));
        
        // client
        row++;
        pane.add(clientRoleLabel, gbc(row, 0, 1, false));
        row++;
        pane.add(clientHostPortLabel, gbc(row, 0, 1, false));
        pane.add(clientHostPortText, gbc(row, 1, 1, true));
        row++;
        
        // pull
        pane.add(toPullLabel, gbc(row, 0, 1, false));
        row++;
        pane.add(pullRemotePathsLabel, gbc(row, 0, 1, false));
        pane.add(pullRemotePathsText, gbc(row, 1, 1, true));
        row++;
        pane.add(pullLocalDirLabel, gbc(row, 0, 1, false));
        pane.add(pullLocalDirText, gbc(row, 1, 1, true));
        pane.add(browseButton, gbc(row, 2, 1, false));
        row++;
        row++;
        pane.add(excludesLabel, gbc(row, 0, 1, false));
        pane.add(excludesText, gbc(row, 1, 1, true));
        row++;
        pane.add(matchesLabel, gbc(row, 0, 1, false));
        pane.add(matchesText, gbc(row, 1, 1, true));
        row++;
        pane.add(pullDryrunButton, gbc(row, 2, 1, false));
        pane.add(pullDiffButton, gbc(row, 3, 1, false));
        pane.add(pullButton, gbc(row, 4, 1, false));
        row++;
        
        // be pulled
        pane.add(toBePulledLabel, gbc(row, 0, 1, false));
        pane.add(bePulledButton, gbc(row, 1, 1, false));
        row++;

        // Add the buttons and the log to this panel,
        add(pane, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == pullButton || e.getSource() == pullDryrunButton) {
            if (alreadyHaveAChild()) {
                return;
            }
            String serverString = Gui.this.serverText.getText();
            String remotePath = Gui.this.remotePathText.getText();
            String localPath = Gui.this.localPathText.getText();
            String excludePattern = Gui.this.excludeText.getText();
            String matchPattern = Gui.this.matchText.getText();
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
            String localPort = Gui.this.portText.getText();
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
        java.net.URL imgURL = Gui.class.getResource(path);
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
        frame.add(new Gui());
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
