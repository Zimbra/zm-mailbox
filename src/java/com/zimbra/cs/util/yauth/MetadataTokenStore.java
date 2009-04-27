/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.io.IOException;

public class MetadataTokenStore extends TokenStore {
    private final Mailbox mbox;
    private final Map<String, String> tokens;

    private static final Log LOG = ZimbraLog.datasource;

    private static final String YAUTH_KEY = "YAUTH";
    private static final String VERSION_KEY = "VERSION";
    private static final String TOKENS_KEY = "TOKENS";

    private static final long VERSION = 2;

    public static void clearTokens(Mailbox mbox) throws ServiceException {
        mbox.setConfig(new Mailbox.OperationContext(mbox), YAUTH_KEY, null);
    }
    
    public static void copyTokens(Mailbox from, Mailbox to) throws ServiceException {
        Metadata md = from.getConfig(new Mailbox.OperationContext(from), YAUTH_KEY);
        if (md != null) {
            LOG.debug("Copying auth tokens from mailbox '%s' to '%s'", from.getId(), to.getId());
            to.setConfig(new Mailbox.OperationContext(to), YAUTH_KEY, md);
        }
    }

    public MetadataTokenStore(Mailbox mbox) throws ServiceException {
        this.mbox = mbox;
        tokens = new HashMap<String, String>();
        loadTokens();
    }

    @Override
    public String newToken(String appId, String user, String pass) throws AuthenticationException, IOException {
        LOG.debug("Generating new token for user '%s'", user);
        return super.newToken(appId, user, pass);
    }
    
    public String getToken(String appId, String user) {
        // LOG.debug("Getting token for user '%s'", user);
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

    public void removeToken(String appId, String user) {
        synchronized (this) {
            tokens.remove(key(appId, user));
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
        } else {
            LOG.debug("Creating new token store for mailbox '%s'", mbox.getId());
        }
    }

    private void loadTokens(MetadataList ml) throws ServiceException {
        int size = ml.size();
        for (int i = 0; i < size; i++) {
            String[] parts = ml.get(i).split(" ");
            if (parts.length == 3) {
                String appId = parts[0];
                String user = parts[1];
                String token = parts[2];
                // LOG.debug("Loaded token: appId=%s, user=%s, token=%s", appId, user, token);
                tokens.put(key(appId, user), token);
            }
        }
        LOG.debug("Loaded %d yauth token(s) for mailbox '%s'", size, mbox.getId());
    }

    private void saveTokens() {
        LOG.debug("Saving %d yauth token(s) for mailbox '%s'", tokens.size(), mbox.getId());
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
            /*
            if (LOG.isDebugEnabled()) {
                String[] parts = e.getKey().split(" ");
                LOG.debug("Saved token: appId=%s, user=%s, token=%s",
                          parts[0], parts[1], e.getValue());
            }
            */
        }
        return ml;
    }

    private static String key(String appId, String user) {
        return appId + " " + user;
    }
}
