package com.tpsup.tpdist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.Border;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class Gui extends JPanel implements ActionListener {
	// https://www.baeldung.com/java-serial-version-uid
	private static final long serialVersionUID = 1L;

	// log area
	JTextArea logArea;
	// server
	JTextField serverPortText;
	JRadioButton serverModeRadio;
	// client
	JTextField clientHostPortText;
	JRadioButton clientModeRadio;
	// pull
	JTextField pullRemotePathsText, pullLocalDirText, excludesText, matchesText;
	JButton pullButton, pullDryrunButton, pullDiffButton, browseButton;
	// to-be-pulled
	JButton bePulledButton;
	// encode
	JTextField keyText;
	// verbose
	JRadioButton verboseModeRadio;

	JFileChooser fileChooser;

	public Gui() {
		super(new BorderLayout());
		// Create the log first, because the action listeners need to refer to it.
		JTextArea logArea = new JTextArea(30, 60);
		logArea.setMargin(new Insets(5, 5, 5, 5));
		logArea.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(logArea);
		MyLog.jtextarea = logArea;

		// client
		// https://www.geeksforgeeks.org/java-swing-jtogglebutton-class/
		clientModeRadio = new JRadioButton("Client mode, Remote Server Host:Port");
		clientModeRadio.setSelected(true);
		clientModeRadio.addActionListener(this);
		clientHostPortText = new JTextField();
		clientHostPortText.setText("localhost:5555");

		// server
		JLabel RoleLabel = new JLabel("Select a role: server or client");

		serverModeRadio = new JRadioButton("Server mode, Local Listening Port");
		serverModeRadio.addActionListener(this);
		serverPortText = new JTextField();
		serverPortText.setText("5555");
		// pull
		JLabel toPullLabel = new JLabel("To pull:");

		JLabel pullRemotePathsLabel = new JLabel("Remote Paths");
		JLabel pullRemotePathsLabel2 = new JLabel("separated by '|'");
		pullRemotePathsText = new JTextField();
		pullRemotePathsText.setText("C:/Users/william/testdir");

		JLabel pullLocalDirLabel = new JLabel("Local Dir");
		pullLocalDirText = new JTextField();
		pullLocalDirText.setText("C:/Users/william/client");
		browseButton = new JButton("Browse");

		JLabel excludesLabel = new JLabel("RegEx Exclude Patterns");
		JLabel excludesLabel2 = new JLabel("Optional, separated by ','");
		excludesText = new JTextField();
		excludesText.setText("");

		JLabel matchesLabel = new JLabel("RegEx Match Patterns");
		JLabel matchesLabel2 = new JLabel("optional, separated by ','");
		matchesText = new JTextField();
		matchesText.setText("");

		pullDryrunButton = new JButton("Dryrun");
		pullDryrunButton.addActionListener(this);

		pullDiffButton = new JButton("Dryrun with Diff");
		pullDiffButton.addActionListener(this);

		pullButton = new JButton("Pull");
		pullButton.addActionListener(this);

		// be pulled
		JLabel toBePulledLabel = new JLabel("To be pulled:");
		bePulledButton = new JButton("Wait to be pulled");
		bePulledButton.addActionListener(this);

		// others
		JLabel encrptionLabel = new JLabel("Encryption Key");
		keyText = new JTextField();
		keyText.setText("");
		verboseModeRadio = new JRadioButton("Verbose mode");
		verboseModeRadio.addActionListener(this);

		// For layout purposes, put the buttons in a separate panel
		JPanel panel = new JPanel(new GridBagLayout());
		// use FlowLayout
		int row = 0;

		// client
		panel.add(clientModeRadio, gbc(row, 0, 1, false));
		panel.add(clientHostPortText, gbc(row, 1, 1, true));

		// server
		row++;
		panel.add(RoleLabel, gbc(row, 0, 1, false));
		row++;
		panel.add(serverModeRadio, gbc(row, 0, 1, false));
		panel.add(serverPortText, gbc(row, 1, 1, true));

		row++;
		panel.add(new JSeparator(), gbc(row, 0, 1, true));

		// pull
		row++;
		panel.add(toPullLabel, gbc(row, 0, 1, false));
		row++;
		panel.add(pullRemotePathsLabel, gbc(row, 0, 1, false));
		panel.add(pullRemotePathsText, gbc(row, 1, 1, true));
		panel.add(pullRemotePathsLabel2, gbc(row, 2, 1, true));
		row++;
		panel.add(pullLocalDirLabel, gbc(row, 0, 1, false));
		panel.add(pullLocalDirText, gbc(row, 1, 1, true));
		panel.add(browseButton, gbc(row, 2, 1, false));
		row++;
		panel.add(excludesLabel, gbc(row, 0, 1, false));
		panel.add(excludesText, gbc(row, 1, 1, true));
		panel.add(excludesLabel2, gbc(row, 2, 1, true));
		row++;
		panel.add(matchesLabel, gbc(row, 0, 1, false));
		panel.add(matchesText, gbc(row, 1, 1, true));
		panel.add(matchesLabel2, gbc(row, 2, 1, true));
		row++;
		panel.add(pullDryrunButton, gbc(row, 0, 1, false));
		panel.add(pullDiffButton, gbc(row, 1, 1, false));
		panel.add(pullButton, gbc(row, 2, 1, false));

		row++;
		panel.add(new JSeparator(), gbc(row, 0, 1, true));

		// be pulled
		row++;
		panel.add(toBePulledLabel, gbc(row, 0, 1, false));
		panel.add(bePulledButton, gbc(row, 1, 1, false));

		row++;
		panel.add(new JSeparator(), gbc(row, 0, 1, true));

		// others
		row++;
		panel.add(encrptionLabel, gbc(row, 0, 1, false));
		panel.add(keyText, gbc(row, 1, 1, true));
		row++;
		panel.add(verboseModeRadio, gbc(row, 0, 1, false));

		// Add the buttons and the log to this panel,
		add(panel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
	}

	public HashMap<String, Object> get_pull_params() {
		HashMap<String, Object> map = new HashMap<String, Object>();

		map.put("remote_paths", pullRemotePathsText.getText().split("[|]"));
		map.put("local_dir", pullLocalDirText.getText());

		return map;
	}

	public HashMap<String, Object> get_opt() {
		HashMap<String, Object> opt = new HashMap<String, Object>();

		opt.put("verbose", verboseModeRadio.isSelected());
		opt.put("encode", keyText.getText());
		opt.put("matches", matchesText.getText());
		opt.put("excludes", excludesText.getText());

		return opt;
	}

	public HashMap<String, Object> get_client_params() {
		HashMap<String, Object> map = new HashMap<String, Object>();

		String[] hostPort = clientHostPortText.getText().split(":");
		if (hostPort.length != 2) {
			MyLog.append(MyLog.ERROR, "bad format in client HostPort");
			clientHostPortText.setBorder((Border) Color.RED);
			return null;
		} else {
			// reset color
			// https://stackoverflow.com/questions/43475953/jtextfield-reset-border-to-system-default
			clientHostPortText.setBorder(new JTextField().getBorder());
		}
		String host = hostPort[0];
		Integer port = Integer.parseInt(hostPort[1]);

		map.put("host", host);
		map.put("port", port);

		return map;
	}

	public HashMap<String, Object> get_server_params() {
		HashMap<String, Object> map = new HashMap<String, Object>();

		String serverPortString = serverPortText.getText();

		int serverPort;
		try {
			serverPort = Integer.parseInt(serverPortString);
		} catch (Exception e) {
			MyLog.append(MyLog.ERROR, "bad format in client HostPort");
			clientHostPortText.setBorder((Border) Color.RED);

			// reset color
			// https://stackoverflow.com/questions/43475953/jtextfield-reset-border-to-system-default
			serverPortText.setBorder(new JTextField().getBorder());
			return null;
		}

		map.put("port", serverPort);

		return map;
	}

	public void actionPerformed(ActionEvent e) {
		Object eSource = e.getSource();

		if (eSource == pullButton || eSource == pullDryrunButton || eSource == pullDiffButton) {
			if (alreadyHaveAChild()) {
				MyLog.append(MyLog.ERROR, "there is already a job running. please try later");
				return;
			}

			HashMap<String, Object> pull_params = get_pull_params();
			if (pull_params == null) {
				return;
			}

			HashMap<String, Object> opt = get_opt();
			if (opt == null) {
				return;
			}
			
			Access access = null;
			try {
				access = new Access(opt);
			} catch (Exception e1) {
				MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e1));
				return;
			}		
			
			opt.put("access", access);

			Thread thread;
			String threadName;
			if (clientModeRadio.isSelected()) {
				HashMap<String, Object> client_params = get_client_params();
				if (client_params == null) {
					return;
				}
				RunnableClientPull clientPullRunnerable = new RunnableClientPull(client_params, pull_params, opt);
				thread = new Thread(clientPullRunnerable);
				threadName = (String) client_params.get("host") + "-" + (Integer) client_params.get("port");
			} else {
				HashMap<String, Object> server_params = get_server_params();
				if (server_params == null) {
					return;
				}
				RunnableServerPull serverPullRunnable = new RunnableServerPull(server_params, pull_params, opt);
				thread = new Thread(serverPullRunnable);
				threadName = "listener-" + (Integer) server_params.get("port");
			}
			
			MyLog.append("spawning a thread to pull, threadName = " + threadName);
			thread.setName(threadName);
			thread.start();
		} else if (e.getSource() == bePulledButton) {
		} else if (e.getSource() == browseButton) {
//            int returnVal = fc.showDialog(this, "Select");
//            if (returnVal == JFileChooser.APPROVE_OPTION) {
//                File file = fc.getSelectedFile();
//                // This is where a real application would save the file, 
//                log.append("selected: " + fc.getCurrentDirectory() + "/" + file.getName() + "." + newline);
//                this.localPushPathText.setText(fc.getCurrentDirectory() + "/" + file.getName());
//            } else {
//                log.append("browser is cancelled by user." + newline);
//            }
//            log.setCaretPosition(log.getDocument().getLength());
//		} else if (e.getSource() == bePulledButton) {
//            if (alreadyHaveAChild()) {
//                return;
//            }
//            String SourcePath = this.localPushPathText.getText();
//            String ServerUrl = this.pushServerText.getText();
//            Pattern expectedPattern = Pattern.compile("^\\S+:\\S+$");
//            Matcher matcher = expectedPattern.matcher(ServerUrl);
//            if (matcher.find()) {
//            } else {
//                log.append("Server Host:Port= " + ServerUrl + "in bad format." + newline);
//                return;
//            }
//            log.append("spawning a client thread to connect to " + ServerUrl + newline);
//            ClientPush clientPush = new ClientPush(SourcePath, ServerUrl, log);
//            Thread thread = new Thread(clientPush);
//            thread.setName("port-" + ServerUrl);
//            thread.start();
		}
	}

	private boolean alreadyHaveAChild() {
		Pattern threadPattern = Pattern.compile("port-");
		// https://stackoverflow.com/questions/15370120/get-thread-by-name
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (threadPattern.matcher(t.getName()).find()) {
//                log.append("thread " + t.getName() + " is still running, cannot spawn new thread" + newline);
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
		JFrame frame = new JFrame(Env.projName + " GUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Add content to the window.
		frame.add(new Gui());
		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	private static Object gbc(int row, int column, int span, boolean resizeRight) {
		return new GridBagConstraints(column, row, span, 1, resizeRight ? 1. : 0., 0., GridBagConstraints.WEST,
				resizeRight ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0);
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
