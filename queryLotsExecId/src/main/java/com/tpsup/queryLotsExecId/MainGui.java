package com.tpsup.queryLotsExecId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.*;

public class MainGui extends JPanel implements ActionListener {
    static private final String newline = "\n";
    JTextField configPathText, queryPathText, inputPathText, outputPathText;
    JButton browseQueryButton, saveQueryButton, browseCsvButton, browseOutputButton, browseConfigButton,
            runButton;
    JFileChooser browseQueryFC, saveQueryFC, inputFC, outputFC, configFC;
    JTextArea queryTextArea, log;
    JComboBox<String> connComboBox, dateComboBox, execIdComboBox;
    String connection = null;
    HashMap<String, String> connectionByKey = new HashMap<>();
    String inputFilename = null;
    ArrayList<String> csvHeaders = new ArrayList<>();
    String dateColumn = null, execIdColumn = null;
    JLabel errorLabel;
    String url = null;

    public MainGui() {
        super(new BorderLayout());
        // Create the log first, because the action listeners need to refer to it.
        log = new JTextArea(30, 60);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        // String defaultConfig = "C:/Users/hantian/dca/configure.properties";
        String user = System.getProperty("user.name");
        String defaultDir = "C:/Users/" + user + "/eclipse-workspace/queryLotsExecId/src/main/resources/";
        String defaultConfig = defaultDir + "/configure.properties";
        log.append("config file: " + defaultConfig + "\n");
        Properties properties = Config.getProperties(defaultConfig);
        configPathText = new JTextField(defaultConfig);
        configPathText.setEditable(false);
        configFC = new JFileChooser();
        configFC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        browseConfigButton = new JButton("Select Another Config File");
        browseConfigButton.addActionListener(this);
        JLabel connectionLabel = new JLabel("Connections");
        connectionByKey = Config.getJdbcConnections(defaultConfig);
        Set<String> keys = connectionByKey.keySet();
        connComboBox = new JComboBox(keys.toArray());
        connComboBox.setEditable(true);
        // set index to -1 so that no value displayed until user selects.
        connComboBox.setSelectedIndex(-1);
        connComboBox.addActionListener(this);
        queryTextArea = new JTextArea();
        JLabel queryLabel = new JLabel("Query");
        String queryFilename = properties.getProperty("default.sql");
        log.append("default sql query file = " + queryFilename + newline);
        String queryContent = null;
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(queryFilename));
            queryContent = new String(encoded, StandardCharsets.UTF_8);
            queryTextArea.setText(queryContent);
        } catch (IOException el) {
            el.printStackTrace();
        }
        queryTextArea.setText(queryContent);
        queryPathText = new JTextField(queryFilename);
        queryPathText.setEditable(false);
        browseQueryFC = new JFileChooser();
        browseQueryFC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        browseQueryButton = new JButton("Browse Query");
        browseQueryButton.addActionListener(this);
        saveQueryFC = new JFileChooser();
        saveQueryFC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        saveQueryButton = new JButton("Save Query");
        saveQueryButton.addActionListener(this);
        inputFilename = properties.getProperty("default.input.csv");
        inputPathText = new JTextField(inputFilename);
        inputPathText.setEditable(false);
        inputPathText.addActionListener(this);
        JLabel inputPathLabel = new JLabel("Input CSV Path");
        // csvPathText.setText("C:/Users/hantian/dca/test.csv");
        inputFC = new JFileChooser();
        inputFC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        browseCsvButton = new JButton("Browse for another CSV");
        browseCsvButton.addActionListener(this);
        csvHeaders = Csv.getCsvHeader(inputFilename);
        JLabel dateLabel = new JLabel("Select Date Column");
        dateComboBox = new JComboBox(csvHeaders.toArray());
        dateComboBox.setEditable(false);
        JLabel execIdLabel = new JLabel("Select ExecID Column");
        execIdComboBox = new JComboBox(csvHeaders.toArray());
        execIdComboBox.setEditable(false);
        dateComboBox.setSelectedIndex(-1);
        execIdComboBox.setSelectedIndex(-1);
        dateComboBox.addActionListener(this);
        execIdComboBox.addActionListener(this);
        outputPathText = new JTextField();
        outputPathText.setEditable(false);
        JLabel outputPathLabel = new JLabel("Output CSV Path");
        outputPathText.setText(properties.getProperty("default.output.csv"));
        outputPathText.addActionListener(this);
        outputFC = new JFileChooser();
        outputFC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        browseOutputButton = new JButton("output to another place");
        browseOutputButton.addActionListener(this);
        runButton = new JButton("Run!");
        runButton.addActionListener(this);
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        if (Files.isDirectory(Paths.get(defaultDir))) {
            System.setProperty("user.dir", defaultDir);
            configFC.setCurrentDirectory(new File(defaultDir));
            browseQueryFC.setCurrentDirectory(new File(defaultDir));
            saveQueryFC.setCurrentDirectory(new File(defaultDir));
            inputFC.setCurrentDirectory(new File(defaultDir));
            outputFC.setCurrentDirectory(new File(defaultDir));
        }
        // For layout purposes, put the buttons in a separate panel
        JPanel pane = new JPanel(new GridBagLayout()); // use FlowLayout
        int row = 0;
        row++;
        pane.add(connectionLabel, gbc(row, 0, 1, false));
        pane.add(connComboBox, gbc(row, 1, 1, true));
        row++;
        pane.add(configPathText, gbc(row, 1, 1, true));
        pane.add(browseConfigButton, gbc(row, 2, 1, false));
        row++;
        pane.add(queryLabel, gbc(row, 0, 1, false));
        pane.add(queryTextArea, gbc(row, 1, 2, true));
        row++;
        pane.add(queryPathText, gbc(row, 1, 1, true));
        pane.add(browseQueryButton, gbc(row, 2, 1, false));
        pane.add(saveQueryButton, gbc(row, 3, 1, false));
        row++;
        pane.add(inputPathLabel, gbc(row, 0, 1, false));
        pane.add(inputPathText, gbc(row, 1, 1, true));
        pane.add(browseCsvButton, gbc(row, 2, 1, false));
        row++;
        pane.add(dateLabel, gbc(row, 1, 1, false));
        pane.add(dateComboBox, gbc(row, 2, 1, true));
        row++;
        pane.add(execIdLabel, gbc(row, 1, 1, false));
        pane.add(execIdComboBox, gbc(row, 2, 1, true));
        row++;
        pane.add(outputPathLabel, gbc(row, 0, 1, false));
        pane.add(outputPathText, gbc(row, 1, 1, true));
        pane.add(browseOutputButton, gbc(row, 2, 1, false));
        row++;
        pane.add(runButton, gbc(row, 3, 1, false));
        row++;
        pane.add(errorLabel, gbc(row, 0, 1, false));
        // Add the buttons and the log to this panel,
        add(pane, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == runButton) {
            log.append(newline);
            String key = (String) connComboBox.getSelectedItem();
            if (key == null) {
                errorLabel.setText("Error: must select a Connection");
                return;
            } else {
                log.append("connection: " + key + newline);
            }
            String conn = connectionByKey.get(key);
            String query = (String) queryTextArea.getText();
            if (query.isEmpty()) {
                errorLabel.setText("Error: must select a query");
                return;
            } else {
                log.append(newline + "query: " + query + newline + newline);
            }
            String csvFilename = (String) inputPathText.getText();
            if (csvFilename.isEmpty()) {
                errorLabel.setText("Error: must select a csv file");
                return;
            } else {
                log.append("input csv" + csvFilename + newline);
            }
            String dateColumn = (String) dateComboBox.getSelectedItem();
            if (dateColumn == null) {
                errorLabel.setText("Error: must select a date column from the csv file");
                return;
            } else {
                log.append("date column: " + dateColumn + newline);
            }
            String execIdColumn = (String) execIdComboBox.getSelectedItem();
            if (execIdColumn == null) {
                errorLabel.setText("Error: must select a ExecId column from the csv file");
                return;
            } else {
                log.append("ExecID column : " + execIdColumn + newline);
            }
            String outputFilename = (String) outputPathText.getText();
            if (outputFilename == null) {
                errorLabel.setText("Error: must specify an output filename to output");
                return;
            } else {
                log.append("output csv : " + outputFilename + newline);
            }
            if (alreadyHaveAChild()) {
                return;
            }
            log.append("spawning a query thread " + newline);
            Process process = new Process(conn, query, csvFilename, dateColumn, execIdColumn, outputFilename,
                    log, null);
            Thread thread = new Thread(process);
            thread.setName("query");
            thread.start();
        } else if (e.getSource() == browseQueryButton) {
            int returnVal = browseQueryFC.showDialog(this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = browseQueryFC.getSelectedFile();
                String queryFilename = browseQueryFC.getCurrentDirectory() + "/" + file.getName();
                log.append("selected query: " + queryFilename + newline);
                try {
                    byte[] encoded = Files.readAllBytes(Paths.get(queryFilename));
                    String queryContent = new String(encoded, StandardCharsets.UTF_8);
                    queryTextArea.setText(queryContent);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
                queryPathText.setText(queryFilename);
                saveQueryFC.setCurrentDirectory(browseQueryFC.getCurrentDirectory());
            } else {
                log.append("query browser is cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == saveQueryButton) {
            int returnVal = saveQueryFC.showDialog(this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = saveQueryFC.getSelectedFile();
                String queryFilename = saveQueryFC.getCurrentDirectory() + "/" + file.getName();
                log.append("will save query to: " + queryFilename + newline);
                String queryContent = queryTextArea.getText();
                try (PrintWriter out = new PrintWriter(queryFilename)) {
                    out.println(queryContent);
                    out.close();
                } catch (IOException el) {
                    el.printStackTrace();
                    return;
                }
                queryPathText.setText(queryFilename);
                browseQueryFC.setCurrentDirectory(saveQueryFC.getCurrentDirectory());
            } else {
                log.append("query browser is cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == browseCsvButton || e.getSource() == inputPathText) {
            dateComboBox.removeAllItems();
            execIdComboBox.removeAllItems();
            if (e.getSource() == browseCsvButton) {
                int returnVal = inputFC.showDialog(this, "Select");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = inputFC.getSelectedFile();
                    inputFilename = inputFC.getCurrentDirectory() + "/" + file.getName();
                } else {
                    log.append("csv browser is cancelled by user." + newline);
                    return;
                }
            } else {
                inputFilename = inputPathText.getText();
                if (!Files.isRegularFile(Paths.get(inputFilename))) {
                    errorLabel.setText("Error: " + inputFilename + " not found" + newline);
                    return;
                }
            }
            log.append("selected csv: " + inputFilename + newline);
            this.inputPathText.setText(inputFilename);
            int max_lines = 3;
            int i = 0;
            log.append("the first " + max_lines + " lines of this csv: " + newline + newline);
            try {
                FileInputStream fstream = new FileInputStream(inputFilename);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                while ((strLine = br.readLine()) != null && i < max_lines) {
                    i++;
                    log.append(strLine + newline);
                }
                log.append(newline);
                fstream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            csvHeaders = Csv.getCsvHeader(inputFilename);
            for (String key : csvHeaders) {
                dateComboBox.addItem(key);
            }
            dateComboBox.setSelectedIndex(-1);
            for (String key : csvHeaders) {
                execIdComboBox.addItem(key);
            }
            execIdComboBox.setSelectedIndex(-1);
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == browseConfigButton) {
            int returnVal = configFC.showDialog(this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = configFC.getSelectedFile();
// This is where a real application would output the file,
                log.append("selected config: " + configFC.getCurrentDirectory() + "/" + file.getName() + "."
                        + newline);
                this.configPathText.setText(configFC.getCurrentDirectory() + "/" + file.getName());
                connectionByKey = Config.getJdbcConnections(file.toString());
                connComboBox.removeAllItems();
                for (String key : connectionByKey.keySet()) {
                    connComboBox.addItem(key);
                }
                connComboBox.setSelectedIndex(-1);
            } else {
                log.append("config browser is cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == browseOutputButton) {
            int returnVal = outputFC.showDialog(this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = outputFC.getSelectedFile();
// This is where a real application would output the file, 
                log.append("will output to: " + outputFC.getCurrentDirectory() + "/" + file.getName() + "."
                        + newline);
                this.outputPathText.setText(browseQueryFC.getCurrentDirectory() + "/" + file.getName());
            } else {
                log.append("output browser is cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == connComboBox) {
            JComboBox cb = (JComboBox) e.getSource();
            String key = (String) cb.getSelectedItem();
            String url = connectionByKey.get(key);
            log.append("selected connection : " + key + newline);
        } else if (e.getSource() == dateComboBox) {
            JComboBox cb = (JComboBox) e.getSource();
            dateColumn = (String) cb.getSelectedItem();
            log.append("selected dateColumn : " + dateColumn + newline);
        } else if (e.getSource() == execIdComboBox) {
            JComboBox cb = (JComboBox) e.getSource();
            execIdColumn = (String) cb.getSelectedItem();
            log.append("selected execIdColumn : " + execIdColumn + newline);
        }
    }

    private boolean alreadyHaveAChild() {
        Pattern threadPattern = Pattern.compile("query");
// https://stackoverflow.com/questions/15370120/get-thread-by-name
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (threadPattern.matcher(t.getName()).find()) {
                log.append("thread " + t.getName() + " is still running, cannot spawn new thread" + newline);
                return true;
            }
        }
        return false;
    }

    /** Pxeturns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = MainGui.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * 
     * Create the GUI and show it. For thread safety, this method should be invoked
     * 
     * from the event dispatch thread.
     * 
     */
    private static void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("FileCopy GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Add content to the window,
        frame.add(new MainGui());
        // Display the window,
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
