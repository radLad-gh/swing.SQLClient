/*
Name: Justice Smith
Course: CNT 4714 Spring 2023
Assignment title: Project 3 – A Two-tier Client-Server Application
Date: March 9, 2023
Class: Enterprise Computing
*/

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

public class SQLClient {
    private JLabel connectionStatusLabel;
    private JTextField username;
    private JPasswordField password;
    private JComboBox<String> propDropdown;
    private DBConnector connector;
    private JTextArea inputArea;
    static final String DEFAULT_QUERY = "SELECT * FROM bikes";
    JTable resultTable;

    SQLClient() {
        JFrame app = new JFrame("SQLClient");
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        app.getContentPane().add(composeContent());

        app.pack();
        app.setVisible(true);
    }

    private static void runGUI() {
        new SQLClient();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(SQLClient::runGUI);
    }

    JPanel composeContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));

        connector = DBConnector.getInstance();
        JPanel connectionPanel = createConnectorPanel();
        JPanel inputPanel = createInputPanel();
        JPanel outputPanel = createOutputPanel();

        content.add(connectionPanel, getConnectionPanelConstraints());
        content.add(inputPanel, getInputPanelConstraints());
        content.add(outputPanel, getOutputPanelConstraints());

        return content;
    }

    JPanel createConnectorPanel() {
        JPanel connectorPanel = new JPanel();
        connectorPanel.setLayout(new GridBagLayout());
        connectorPanel.setPreferredSize(new Dimension(350, 150));
        connectorPanel.setBorder(BorderFactory.createTitledBorder("Connection Details"));
        GridBagConstraints c;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        connectorPanel.add(new JLabel("Properties File:", SwingConstants.CENTER), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        String[] propFiles = new String[]{"", "root.properties", "client.properties"};
        propDropdown = new JComboBox<>(propFiles);
        connectorPanel.add(propDropdown, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        connectorPanel.add(new JLabel("Username:", SwingConstants.CENTER), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        username = new JTextField("");
        connectorPanel.add(username, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        connectorPanel.add(new JLabel("Password:", SwingConstants.CENTER), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        password = new JPasswordField("");
        connectorPanel.add(password, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        String DEFAULT_CONNECTION_STATUS = "Disconnected";
        connectionStatusLabel = new JLabel("Status: " + DEFAULT_CONNECTION_STATUS, SwingConstants.CENTER);
        connectionStatusLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
        connectorPanel.add(connectionStatusLabel, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        JButton dbConnectButton = new JButton("Connect to Database");
        dbConnectButton.addActionListener(e -> {
            String un = username.getText();
            char[] pw = password.getPassword();

            Object propertiesFileName = propDropdown.getSelectedItem();
            String file = "./properties/" + propertiesFileName;
            System.out.println(file);

            try {
                // Assemble reference key
                Properties props = retrieveCredentials(file);

                // Validate credentials
                if (!userCredentialsValid(un, String.valueOf(pw), props)) {
                    JOptionPane.showMessageDialog(null, "Properties file does not match credentials provided", "Authentication Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Connect to DB
                connector.connect(props);

                // Connection failure
                if (!connector.isConnected()) {
                    throw new SQLException("Database not connected");
                }

                updateConnectionStatus(connector.getConnectionStatus());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
            }
        });
        connectorPanel.add(dbConnectButton, c);

        return connectorPanel;
    }

    JPanel createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter an SQL Command"));
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setPreferredSize(new Dimension(350, 150));

        GridBagConstraints c;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 5);
        inputArea = new JTextArea(DEFAULT_QUERY);
        inputArea.setMargin(new Insets(5, 10, 5, 10));
        inputArea.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, c);


        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.5;
        c.insets = new Insets(5, 0, 0, 0);
        JButton clearCommand = new JButton("Clear SQL Command");
        clearCommand.addActionListener(e -> inputArea.setText(""));
        inputPanel.add(clearCommand, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.5;
        c.insets = new Insets(5, 0, 0, 0);
        JButton executeCommand = new JButton("Execute SQL Command");
        executeCommand.addActionListener(e -> {
            connector = DBConnector.getInstance();
            if (!connector.isConnected()) {
                return;
            }
            String query = inputArea.getText();
            try {
                if (query.split(" ")[0].equalsIgnoreCase("select")) {
                    connector.setQuery(query);
                } else {
                    connector.setUpdate(query);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        inputPanel.add(executeCommand, c);

        return inputPanel;
    }

    JPanel createOutputPanel() {
        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new GridBagLayout());
        outputPanel.setPreferredSize(new Dimension(350, 250));
        outputPanel.setBorder(BorderFactory.createTitledBorder("SQL Execution Result"));
        GridBagConstraints c;

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.insets = new Insets(5, 5, 0, 5);
        resultTable = new JTable(connector);
        resultTable.setGridColor(Color.BLACK);
        JScrollPane outputScroll = new JScrollPane(resultTable);
        outputScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        outputPanel.add(outputScroll, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0;
        c.weightx = 1;
        c.insets = new Insets(5, 5, 5, 5);
        JButton clearWindow = new JButton("Clear Result Window");
        clearWindow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                model.setRowCount(0);
                model.fireTableDataChanged();
            }
        });
        outputPanel.add(clearWindow, c);

        return outputPanel;
    }

    private void updateConnectionStatus(String status) {
        connectionStatusLabel.setText("Status: " + status);
    }

    public Properties retrieveCredentials(String file) {
        Properties props = null;
        try {
            FileInputStream fin;
            props = new Properties();
            fin = new FileInputStream(file);
            props.load(fin);
        } catch (final IOException readErr) {
            JOptionPane.showMessageDialog(null, readErr.getMessage(), "Could not load credentials from file", JOptionPane.ERROR_MESSAGE);
        }

        return props;
    }

    public boolean userCredentialsValid(String un, String pw, Properties props) {
        String MYSQL_DB_USERNAME = "MYSQL_DB_USERNAME";
        String MYSQL_DB_PASSWORD = "MYSQL_DB_PASSWORD";
        return props.getProperty(MYSQL_DB_USERNAME).equals(un) && props.getProperty(MYSQL_DB_PASSWORD).equals(pw);
    }

    public GridBagConstraints getConnectionPanelConstraints() {
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(5, 5, 0, 0);

        return c;
    }

    public GridBagConstraints getInputPanelConstraints() {
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.insets = new Insets(5, 0, 0, 5);

        return c;
    }

    public GridBagConstraints getOutputPanelConstraints() {
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 5, 5, 5);

        return c;
    }
}
