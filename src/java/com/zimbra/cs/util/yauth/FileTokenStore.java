/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util.yauth;

import org.apache.log4j.Logger;

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
import java.util.HashMap;

public class FileTokenStore extends TokenStore {
    private final File file;
    private final Map<String, String> tokens;

    private static final Logger LOG = Logger.getLogger(FileTokenStore.class);

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

    public void putToken(String appId, String user, String token) {
        synchronized (this) {
            tokens.put(key(appId, user), token);
            saveTokens();
        }
    }

    public int size() {
        return tokens.size();
    }

    private void saveTokens() {
        LOG.debug("Saving tokens to '" + file + "'");
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
                    LOG.error("Error closing tokens file '" + file + "'", e);
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
            LOG.debug("Loading auth tokens from '" + file + "'");
            readTokens(r);
        } catch (FileNotFoundException e) {
            LOG.debug("No previously saved auth tokens in '" + file + "'");
            // Fall through...
        } catch (IOException e) {
            // Invalid token file
            LOG.info("Deleting invalid tokens file '" + file + "'");
            e.printStackTrace(); // DEBUG
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
            LOG.debug(String.format("Read token appId=%s, user=%s, token=%s",
                parts[0], parts[1], parts[2]));
            tokens.put(key(parts[0], parts[1]), parts[2]);
        }
    }

    private static String key(String appId, String user) {
        return appId + " " + user;
    }
}
