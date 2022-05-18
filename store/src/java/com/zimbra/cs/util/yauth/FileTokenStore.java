/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util.yauth;


import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class FileTokenStore extends TokenStore {
    private final File file;
    private final Map<String, String> tokens;

    private static final    Logger LOG = LogManager.getLogger(FileTokenStore.class);

    public FileTokenStore(File file) throws IOException {
        this.file = file;
        tokens = new HashMap<String, String>();
        loadTokens();
    }

    public String getToken(String appId, String user) {
        synchronized (this) {
            return tokens.get(key(appId, user));
        }
    }

    protected void putToken(String appId, String user, String token) {
        synchronized (this) {
            tokens.put(key(appId, user), token);
            saveTokens();
        }
    }

    public void removeToken(String appId, String user) {
        synchronized (this) {
            tokens.remove(key(appId, user));
        }
    }

    public int size() {
        return tokens.size();
    }

    private void saveTokens() {
        debug("Saving yauth tokens to file '%s'", file);
        Writer w = null;
        try {
            w = new FileWriter(file);
            writeTokens(w);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save tokens", e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e) {
                    debug("Error closing tokens file '" + file + "'", e);
                }
            }
        }
    }

    private void writeTokens(Writer w) throws IOException {
        BufferedWriter bw = new BufferedWriter(w);
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            bw.write(e.getKey());
            bw.write(' ');
            bw.write(e.getValue());
            bw.newLine();
        }
        bw.flush();
    }

    private void loadTokens() throws IOException {
        Reader r = null;
        try {
            r = new FileReader(file);
            readTokens(r);
            debug("Loaded yauth tokens from file '%s'", file);
        } catch (FileNotFoundException e) {
            // Fall through...
        } catch (IOException e) {
            // Invalid token file
            debug("Deleting invalid tokens file ''%s'", file);
            r.close();
            file.delete();
        } finally {
            if (r != null) {
                r.close();
            }
        }
    }

    private void readTokens(Reader r) throws IOException {
        BufferedReader br = new BufferedReader(r);
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(" ");
            if (parts.length != 3) {
                throw new IOException("Invalid token file");
            }
            //debug("Read token appId=%s, user=%s, token=%s",
            //      parts[0], parts[1], parts[2]));
            tokens.put(key(parts[0], parts[1]), parts[2]);
        }
    }

    private static void debug(String fmt, Object... args) {
        LOG.debug(String.format(fmt, args));
    }
    
    private static String key(String appId, String user) {
        return appId + " " + user;
    }
}
