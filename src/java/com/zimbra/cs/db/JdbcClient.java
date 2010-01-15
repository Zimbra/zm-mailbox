/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command-line utility used for running SQL statements.  Connects to a database
 * server using the JDBC driver and URL specified on the command line.  Displays
 * results of SELECT statements as either formatted or tab-separated.  For DML
 * statements, shows number of rows affected.
 *   
 * @author bburtin
 */
public class JdbcClient {

    private static final String OPT_HELP = "h";
    private static final String OPT_USER = "u";
    private static final String OPT_PASSWORD = "p";
    private static final String OPT_JDBC_URL = "j";
    private static final String OPT_DRIVER_CLASS = "c";
    private static final String OPT_BATCH = "B";
    private static final String OPT_SKIP_COLUMN_NAMES = "N";
    
    private static final String NULL = "NULL";
    
    private static final Pattern PAT_QUIT = Pattern.compile("\\s*quit;?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_EXIT = Pattern.compile("\\s*exit;?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_SELECT =
        Pattern.compile("^\\s*SELECT", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_SEMICOLON = Pattern.compile("([^;]*);(.*)");

    private Options mOptions = new Options();
    private String mDriverClass;
    private String mUser;
    private String mPassword;
    private String mJdbcUrl;
    private boolean mBatch = false;
    private boolean mShowColumnNames = true;
    
    private JdbcClient(String[] args) {
        Option opt = new Option(OPT_HELP, "help", false, "Display this help message.");
        mOptions.addOption(opt);

        opt = new Option(OPT_DRIVER_CLASS, "driver-class", true, "JDBC driver class name.");
        opt.setRequired(true);
        mOptions.addOption(opt);
        
        opt = new Option(OPT_USER, "user", true, "User name.");
        opt.setRequired(true);
        mOptions.addOption(opt);
        
        opt = new Option(OPT_PASSWORD, "password", true, "Password.");
        opt.setRequired(true);
        mOptions.addOption(opt);
        
        opt = new Option(OPT_JDBC_URL, "jdbc-url", true, "JDBC URL used for connecting to the database server.");
        opt.setRequired(true);
        mOptions.addOption(opt);
        
        mOptions.addOption(OPT_BATCH, "batch", false, "Prints results without formatting, separated by tabs.");
        mOptions.addOption(OPT_SKIP_COLUMN_NAMES, "skip-column-names", false, "Don't write column names in results.");

        CommandLine cl = null;
        
        try {
            GnuParser parser = new GnuParser();
            cl = parser.parse(mOptions, args);
        } catch (ParseException e) {
            usage(e);
            System.exit(1);
        }
        
        if (cl.hasOption(OPT_HELP)) {
            usage(null);
            System.exit(0);
        }

        mDriverClass = cl.getOptionValue(OPT_DRIVER_CLASS);
        mUser = cl.getOptionValue(OPT_USER);
        mPassword = cl.getOptionValue(OPT_PASSWORD);
        mJdbcUrl = cl.getOptionValue(OPT_JDBC_URL);
        
        if (cl.hasOption(OPT_BATCH)) {
            mBatch = true;
        }
        if (cl.hasOption(OPT_SKIP_COLUMN_NAMES)) {
            mShowColumnNames = false;
        }
    }
    
    private void run() {
        // Load driver
        try {
            Class.forName(mDriverClass);
        } catch (Throwable t) {
            System.err.println("Unable to load driver '" + mDriverClass + "': " + t);
            System.exit(1);
        }
        
        // Connect
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(mJdbcUrl, mUser, mPassword);
        } catch (SQLException e) {
            System.err.println("Unable to connect to " + mJdbcUrl + " using " + mDriverClass + ": " + e);
            System.exit(1);
        }
        
        // Read SQL statements from stdin
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = "";
            StringBuilder b = new StringBuilder();
            while (line != null) {
                // Exit if necessary
                Matcher quitMatcher = PAT_QUIT.matcher(line);
                Matcher exitMatcher = PAT_EXIT.matcher(line);
                if (quitMatcher.matches() || exitMatcher.matches()) {
                    break;
                }

                line = DbUtil.removeComments(line);
                Matcher semicolonMatcher = PAT_SEMICOLON.matcher(line);
                if (semicolonMatcher.matches()) {
                    // Split line on semicolon
                    b.append(semicolonMatcher.group(1));
                    runSql(conn, b.toString());
                    b = new StringBuilder();
                    line = semicolonMatcher.group(2);
                } else {
                    b.append(line);
                    line = reader.readLine();
                }
            }
            
            // Run last statement
            if (b.length() > 0) {
                runSql(conn, b.toString());
            }
            
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }
        
        try {
            conn.close();
        } catch (SQLException e) {
            System.err.println(e);
            System.exit(1);
        }
    }
    
    private void runSql(Connection conn, String sql) {
        Matcher m = PAT_SELECT.matcher(sql);
        
        if (m.find()) {
            // Run query and display results 
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                List<Object[]> firstRows = new ArrayList<Object[]>();
                int rowCount = 0;

                // Set initial column widths based on column labels
                int[] colWidths = new int[colCount];
                if (mShowColumnNames) {
                    for (int i = 0; i < colCount; i++) {
                        String name = md.getColumnLabel(i+1);
                        if (name.length() > colWidths[i]) {
                            colWidths[i] = name.length();
                        }
                    }
                }
                
                // Read first 1000 rows first to calculate column widths for printing
                while (rowCount < 1000 && rs.next()) {
                    Object[] row = getCurrentRow(rs);
                    for (int i = 0; i < colCount; i++) {
                        Object o = row[i];
                        int width = (o == null) ? NULL.length() : (o.toString()).length();
                        if (width > colWidths[i]) {
                            colWidths[i] = width;
                        }
                    }
                    firstRows.add(row);
                    rowCount++;
                }

                // Print first rows
                if (!mBatch && mShowColumnNames) {
                    // Skip if we're in batch mode.  If not displaying column names, don't
                    // print the first divider.
                    printDivider(colWidths);
                }
                if (mShowColumnNames) {
                    String[] colNames = new String[colCount];
                    for (int i = 0; i < colCount; i++) {
                        colNames[i] = md.getColumnLabel(i + 1);
                    }
                    printRow(colNames, colWidths);
                }
                if (!mBatch) {
                    printDivider(colWidths);
                }
                for (Object[] row : firstRows) {
                    printRow(row, colWidths);
                }
                
                // Print any remaining rows
                while (rs.next()) {
                    Object[] row = getCurrentRow(rs);
                    printRow(row, colWidths);
                }
                if (!mBatch) {
                    printDivider(colWidths);
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        } else {
            // Run statement
            try {
                Statement stmt = conn.createStatement();
                int numRows = stmt.executeUpdate(sql);
                stmt.close();
                System.out.println("Updated " + numRows + " rows");
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
    }
    
    private void printDivider(int[] colWidths) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < colWidths.length; i++) {
            b.append('+');
            for (int j = 0; j < colWidths[i] + 2; j++) {
                b.append('-');
            }
        }
        b.append('+');
        System.out.println(b);
    }
        
    private Object[] getCurrentRow(ResultSet rs)
    throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        Object[] row = new Object[colCount];
        for (int i = 0; i < colCount; i++) {
            row[i] = rs.getObject(i+ 1);
        }
        return row;
    }

    private void printRow(Object[] row, int[] colWidths) {
        StringBuilder b = new StringBuilder();
        if (!mBatch) {
            b.append("| ");
        }
        String delimiter = mBatch ? "\t" : " | ";
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                b.append(delimiter);
            }
            Object o = row[i];
            String s = (o == null) ? NULL : o.toString();
            b.append(s);
            if (!mBatch) {
                if (s.length() < colWidths[i]) {
                    for (int j = 0; j < colWidths[i] - s.length(); j++) {
                        b.append(" ");
                    }
                }
            }
        }
        if (!mBatch) {
            b.append(" |");
        }
        System.out.println(b);
    }
    
    private void usage(ParseException e) {
        if (e != null) {
            System.err.println(e + "\n");
        }

        PrintWriter pw = new PrintWriter(System.err, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), "zmsql <options>",
            null, mOptions, formatter.getLeftPadding(), formatter.getDescPadding(), "");
        
    }

    public static void main(String[] args) {
        JdbcClient app = new JdbcClient(args);
        app.run();
    }
}
