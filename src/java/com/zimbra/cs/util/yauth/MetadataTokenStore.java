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

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.service.ServiceException;

import java.util.Map;
import java.util.HashMap;

public class MetadataTokenStore implements TokenStore {
    private final Mailbox mbox;
    private final Map<String, String> tokens;

    private static final Log LOG = ZimbraLog.datasource;

    private static final String YAUTH_KEY = "YAUTH";
    private static final String VERSION_KEY = "VERSION";
    private static final String TOKENS_KEY = "TOKENS";

    private static final long VERSION = 2;

    public MetadataTokenStore(Mailbox mbox) throws ServiceException {
        this.mbox = mbox;
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
            tokens.put(user, token);
            saveTokens();
        }
    }

    public int size() {
        return tokens.size();
    }

    private void loadTokens() throws ServiceException {
        Metadata md = mbox.getConfig(new Mailbox.OperationContext(mbox), YAUTH_KEY);
        if (md != null) {
            long version = md.getLong(VERSION_KEY, 0);
            if (version == VERSION) {
                MetadataList ml = md.getList(TOKENS_KEY);
                if (ml != null) {
                    loadTokens(ml);
                }
            }
        }
    }

    private void loadTokens(MetadataList ml) throws ServiceException {
        LOG.debug("Loading yauth tokens for account '%s'", mbox.getAccountId());
        int size = ml.size();
        for (int i = 0; i < size; i++) {
            String[] parts = ml.get(i).split(" ");
            if (parts.length == 3) {
                tokens.put(key(parts[0], parts[1]), parts[2]);
            }
        }
    }

    private void saveTokens() {
        LOG.debug("Saving yauth tokens for account '%s'", mbox.getAccountId());
        try {
            Metadata md = new Metadata();
            md.put(VERSION_KEY, VERSION);
            md.put(TOKENS_KEY, saveTokens(tokens));
            mbox.setConfig(new Mailbox.OperationContext(mbox), YAUTH_KEY, md);
        } catch (ServiceException e) {
            throw new IllegalStateException("Unable to save tokens", e);
        }
    }

    private MetadataList saveTokens(Map<String, String> tokens) {
        MetadataList ml = new MetadataList();
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            ml.add(e.getKey() + " " + e.getValue());
        }
        return ml;
    }
    
    private static String key(String appId, String user) {
        return appId + " " + user;
    }
}
