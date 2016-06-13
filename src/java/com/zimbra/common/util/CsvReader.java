/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simple CSV file reader.
 * Current implementation is not bullet-proof, but should be close enough.
 * Splits on commas, possibly surrounded by spaces and quotes.
 * Doesn't currently handle escaped quotes ("") or quoted strings that contain
 * commas (e.g. 1,"Zimbra, Inc.",2).
 * 
 * @author bburtin
 */
public class CsvReader {

    private BufferedReader mReader;
    private String[] mColNames;
    private Map<String, String> mCurrent;

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\\"?\\s*,\\s*\\\"?");
    
    public CsvReader(Reader reader)
    throws IOException {
        mReader = new BufferedReader(reader);
        String line = mReader.readLine();
        if (line == null) {
            mReader.close();
            throw new IOException("CSV reader contains no data");
        }
        mColNames = SPLIT_PATTERN.split(line);
    }
    
    /**
     * Returns <code>true</code> if there is another line to read.
     * 
     * @throws IOException if an error occurred while reading the file.
     */
    public boolean hasNext()
    throws IOException {
        String line = mReader.readLine();
        if (line == null) {
            mReader.close();
            return false;
        }
        
        // Populate the current map for the line we just got
        String[] values = SPLIT_PATTERN.split(line);
        int length = Math.min(values.length, mColNames.length);
        if (mCurrent == null) {
            mCurrent = new HashMap<String, String>();
        } else {
            mCurrent.clear();
        }
        for (int i = 0; i < length; i++) {
            mCurrent.put(mColNames[i], values[i]);
        }
        return true;
    }

    /**
     * Returns the names of all the columns in the CSV file.
     */
    public String[] getColNames() {
        return mColNames;
    }

    /**
     * Returns <code>true</code> if the specified column exists.
     */
    public boolean columnExists(String name) {
        for (String currentName : mColNames) {
            if (currentName.equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the value for the specified column in the current
     * line.
     */
    public String getValue(String colName) {
        if (mCurrent == null) {
            throw new IllegalStateException("getValue() called before hasNext()");
        }
        return mCurrent.get(colName);
    }
    
    public void close()
    throws IOException {
        mReader.close();
    }
}
