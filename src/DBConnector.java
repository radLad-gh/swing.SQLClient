/*
Name: Justice Smith
Course: CNT 4714 Spring 2023
Assignment title: Project 3 – A Two-tier Client-Server Application
Date: March 9, 2023
Class: Enterprise Computing
*/

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DBConnector extends DefaultTableModel {
    private static DBConnector instance;
    private static Connection conn;
    private static ResultSetMetaData rsmd;
    private static ResultSet rs;
    private boolean isConnected = false;
    private int numRows;
    private Statement statement;
    private Properties DEFAULT_PROPERTIES;
    static final String DEFAULT_QUERY = "SELECT * FROM bikes";

    private DBConnector() {
        connect();
        setQuery(DEFAULT_QUERY);
    }

    static public DBConnector getInstance() {
        if (instance == null) {
            instance = new DBConnector();
        }
        return instance;
    }

    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println("Error closing connection");
        }
    }

    public String getConnectionStatus() {
        return isConnected ? "Connected" : "Disconnected";
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect() {
        loadDefaultProperties();
        connect(DEFAULT_PROPERTIES);
    }

    private void loadDefaultProperties() {
        try {
            FileInputStream fin;
            DEFAULT_PROPERTIES = new Properties();
            fin = new FileInputStream("./properties/client.properties");
            DEFAULT_PROPERTIES.load(fin);
        } catch (final IOException readErr) {
            JOptionPane.showMessageDialog(null, readErr.getMessage(), "Could not load credentials from file", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void connect(Properties props) {
        String MYSQL_DB_URL = "MYSQL_DB_URL";
        String MYSQL_DB_USERNAME = "MYSQL_DB_USERNAME";
        String MYSQL_DB_PASSWORD = "MYSQL_DB_PASSWORD";
        MysqlDataSource dataSource;

        dataSource = new MysqlDataSource();
        dataSource.setURL(props.getProperty(MYSQL_DB_URL));
        dataSource.setUser(props.getProperty(MYSQL_DB_USERNAME));
        dataSource.setPassword(props.getProperty(MYSQL_DB_PASSWORD));

        try {
            conn = dataSource.getConnection();
            assert conn != null;

            isConnected = true;
            DatabaseMetaData dbmd = conn.getMetaData();
            System.out.println("driver name: " + dbmd.getDriverName());
            System.out.println("driver version: " + dbmd.getDriverVersion());
            System.out.println("driver major version: " + dbmd.getDriverMajorVersion());
            System.out.println("driver minor version: " + dbmd.getDriverMinorVersion());
        } catch (final SQLException sqlEx) {
            JOptionPane.showMessageDialog(null, sqlEx.getMessage(), "Could not connect", JOptionPane.ERROR_MESSAGE);
        }

    }

    // set new database query string
    public void setQuery(String query) throws IllegalStateException {
        if (!isConnected)
            throw new IllegalStateException("Not Connected to Database");
        try {
            statement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            rs = statement.executeQuery(query);
            rsmd = rs.getMetaData();
            rs.last();
            numRows = rs.getRow();
            fireTableStructureChanged();
        } catch (final SQLException sqlEx) {
            JOptionPane.showMessageDialog(null, sqlEx.getMessage(), "Could not execute query", JOptionPane.ERROR_MESSAGE);
        }

    }

    // set new database update-query string
    public void setUpdate(String query) throws SQLException, IllegalStateException {
        if (!isConnected) throw new IllegalStateException("Not Connected to Database");
        try {
            statement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            int numRowsAffected = statement.executeUpdate(query);
            rsmd = rs.getMetaData();
            rs.last();
            numRows = rs.getRow();
            rs.first();
            fireTableStructureChanged();
        } catch (final SQLException sqlEx) {
            JOptionPane.showMessageDialog(null, sqlEx.getMessage(), "Could not execute update", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void printResultSet(ResultSet rs) throws SQLException {
        System.out.println("Results of the query: ... . . . . . . . . . . . . . . . . \n");

        int numCol = rs.getMetaData().getColumnCount();
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            int i = 1;
            while (i < numCol) {
                sb.append(rs.getString(i++)).append("\t\t\t\t");
            }
            sb.append(rs.getString(i)).append("\n");
        }

        System.out.println(sb);
    }

    @Override
    public int getRowCount() {
        if (!isConnected) return 0;
        try {
            rs.last();
            numRows = rs.getRow();
            rs.beforeFirst();
            return numRows;
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getColumnCount() {
        if (!isConnected) return 0;
        try {
            return rsmd.getColumnCount();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (!isConnected) connect();
        try {
            rs.next(); /* fixes a bug in MySQL/Java with date format */
            rs.absolute(row + 1);
            return rs.getObject(column + 1);
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return "";
    }

    // get name of a particular column in ResultSet
    public String getColumnName(int column) throws IllegalStateException {
        if (!isConnected) connect();
        try {
            return rsmd.getColumnName(column + 1);
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        //all cells false
        return false;
    }
}
