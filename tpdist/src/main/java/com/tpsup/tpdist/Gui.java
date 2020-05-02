package com.tpsup.tpdist;

import java.util.HashMap;
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
	JFileChooser fileChooser;
	// to-be-pulled
	JButton bePulledButton;
	// encode
	JTextField keyText;
	// verbose
	JRadioButton verboseModeRadio;
	// interrupt
	JButton interruptButton;

	public final String threadNamePrefix = Env.projName + "-" + "thread-";
	public final Pattern threadPattern = Pattern.compile(threadNamePrefix);

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
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        browseButton = new JButton("or Browse ...");
        browseButton.addActionListener(this);

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
		
		// interrupt
		interruptButton = new JButton("Interrupt");
		interruptButton.addActionListener(this);

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
		row++;
		panel.add(interruptButton, gbc(row, 0, 1, false));

		// Add the buttons and the log to this panel,
		add(panel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
		
	}

	public HashMap<String, Object> get_pull_params() {
		HashMap<String, Object> map = new HashMap<String, Object>();

		map.put("remote_paths", pullRemotePathsText.getText().split("[|]"));
		map.put("local_dir", pullLocalDirText.getText());

		MyLog.append(MyLog.VERBOSE, "pull_params = " + MyGson.toJson(map));
		
		return map;
	}

	public HashMap<String, Object> get_opt() {
		HashMap<String, Object> opt = new HashMap<String, Object>();

		opt.put("verbose", verboseModeRadio.isSelected());
		// set verbose mode as early as possible
		if (verboseModeRadio.isSelected()) {
			Env.verbose = true;
		}
		
		opt.put("encode", keyText.getText());
		opt.put("matches", matchesText.getText());
		opt.put("excludes", excludesText.getText());
		
		MyLog.append(MyLog.VERBOSE, "opt = " + MyGson.toJson(opt));

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
		
		MyLog.append(MyLog.VERBOSE, "client_params = " + MyGson.toJson(map));

		return map;
	}

	public HashMap<String, Object> get_server_params() {
		HashMap<String, Object> map = new HashMap<String, Object>();

		String serverPortString = serverPortText.getText();

		int serverPort;
		try {
			serverPort = Integer.parseInt(serverPortString);
			serverPortText.setForeground(Color.BLACK);
		} catch (Exception e) {
			MyLog.append(MyLog.ERROR, "bad format in server port");
			MyLog.append(MyLog.ERROR, ExceptionUtils.getStackTrace(e));
			serverPortText.setForeground(Color.RED);
			return null;
		}

		map.put("port", serverPort);
		
		MyLog.append(MyLog.VERBOSE, "server_params = " + MyGson.toJson(map));

		return map;
	}

	public void actionPerformed(ActionEvent e) {
		Object eSource = e.getSource();

		if (eSource == pullButton || eSource == pullDryrunButton || eSource == pullDiffButton) {
			if (alreadyHaveAChild()) {
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
				RunnableClientPull runnable = new RunnableClientPull(client_params, pull_params, opt);
				thread = new Thread(runnable);
				threadName = (String) client_params.get("host") + "-" + (Integer) client_params.get("port");
			} else {
				HashMap<String, Object> server_params = get_server_params();
				if (server_params == null) {
					return;
				}
				RunnableServerPull runnable = new RunnableServerPull(server_params, pull_params, opt);
				thread = new Thread(runnable);
				threadName = "listener-" + (Integer) server_params.get("port");
			}
			
			MyLog.append("spawning a thread to pull, threadName = " + threadName);
			thread.setName(threadName);
			thread.start();
		} else if (eSource == bePulledButton) {
			if (alreadyHaveAChild()) {
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
				RunnableClientBePulled runnable = new RunnableClientBePulled(client_params, opt);
				thread = new Thread(runnable);
				threadName = (String) threadNamePrefix + client_params.get("host") + "-" + (Integer) client_params.get("port");
			} else {
				HashMap<String, Object> server_params = get_server_params();
				if (server_params == null) {
					return;
				}
				RunnableServerBePulled runnable = new RunnableServerBePulled(server_params, opt);
				thread = new Thread(runnable);
				threadName = threadNamePrefix + "listener-" + (Integer) server_params.get("port");
			}
			
			MyLog.append("spawning a thread to pull, threadName = " + threadName);
			thread.setName(threadName);
			thread.start();
		} else if (eSource == browseButton) {
          int returnVal = fileChooser.showDialog(this, "Select");
          if (returnVal == JFileChooser.APPROVE_OPTION) {
              File file = fileChooser.getSelectedFile();
              // This is where a real application would save the file, 
              String fullPath = fileChooser.getCurrentDirectory().toString().replace("\\", "/") + "/" + file.getName();
              MyLog.append("selected: " + fullPath);
              this.pullLocalDirText.setText(fullPath);
          } else {
              MyLog.append("browser is cancelled by user.");
          }
          //MyLog.setCaretPosition(log.getDocument().getLength());
		} else if (eSource == clientModeRadio) {
			serverModeRadio.setSelected(false);
		} else if (eSource == serverModeRadio) {
			clientModeRadio.setSelected(false);
		} else if (eSource == interruptButton) {
			for (Thread t : Thread.getAllStackTraces().keySet()) {
				if (threadPattern.matcher(t.getName()).find()) {
					t.interrupt();
				}
			}
		}
	}

	private boolean alreadyHaveAChild() {
		// https://stackoverflow.com/questions/15370120/get-thread-by-name
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (threadPattern.matcher(t.getName()).find()) {
				MyLog.append(MyLog.ERROR, "there is already a job running. please try later");
				return true;
			}
		}
		return false;
	}

//	/** Returns an ImageIcon, or null if the path was invalid. */
//	protected static ImageIcon createImageIcon(String path) {
//		java.net.URL imgURL = Gui.class.getResource(path);
//		if (imgURL != null) {
//			return new ImageIcon(imgURL);
//		} else {
//			MyLog.append(MyLog.ERROR, "Couldn't find file: " + path);
//			return null;
//		}
//	}

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
