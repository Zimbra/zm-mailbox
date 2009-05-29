/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.URLUtil;

public class MtaAuthHost extends AttributeCallback {

    /**
     * check to make sure zimbraMtaAuthHost points to a valid server zimbraServiceHostname
     */
    public void preModify(Map context, String attrName, Object value,
                          Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        MultiValueMod mod = multiValueMod(attrsToModify, Provisioning.A_zimbraMtaAuthHost);
        Set<String> values = mod.valuesSet();
        
        if (mod.adding()) {
            // Add zimbraMtaAuthURL for each auth host being added
            List<String> urlsToAdd = new ArrayList<String>();
            for (String authHost : values) {
                String authUrl = URLUtil.getMtaAuthURL(authHost);
                urlsToAdd.add(authUrl);
            }
            if (urlsToAdd.size() > 0)
                attrsToModify.put("+" + Provisioning.A_zimbraMtaAuthURL, urlsToAdd.toArray(new String[urlsToAdd.size()]));
            
        } else if (mod.replacing()) {
            // Replace zimbraMtaAuthURL for each auth host being replaced
            List<String> urls = new ArrayList<String>();
            for (String authHost : values) {
                String authUrl = URLUtil.getMtaAuthURL(authHost);
                urls.add(authUrl);
            }
            if (urls.size() > 0)
                attrsToModify.put(Provisioning.A_zimbraMtaAuthURL, urls.toArray(new String[urls.size()]));
            
        } else if (mod.removing()) {
            // Remove zimbraMtaAuthURL for each auth host being removed,
            // if the auth host server to be remove no longer exists, just catch the Exception and 
            // remove the corresponding auth url if there is one
            
            if (!isCreate && entry != null) {
                Set<String> curUrls = entry.getMultiAttrSet(Provisioning.A_zimbraMtaAuthURL);
                List<String> urlsToRemove = new ArrayList<String>();
                
                for (String authHost : values) {
                    try {
                        String authUrl = URLUtil.getMtaAuthURL(authHost);
                        if (curUrls.contains(authUrl))
                            urlsToRemove.add(authUrl);
                        
                    } catch (ServiceException e) {
                        for (String curUrl : curUrls) {
                            try {
                                URL url = new URL(curUrl);
                                String urlHost = url.getHost();
                                // just compare the urlHost with the string of the host
                                // we are removing, there is no way to get the A_zimbraServiceHostname
                                // of the server because the server no longer exists
                                if (authHost.equals(urlHost))
                                    urlsToRemove.add(curUrl);
                            } catch (MalformedURLException mue) {
                                // hmm, mailformed url? just remove it
                                urlsToRemove.add(curUrl);
                            }
                        }
                    }
                }
                if (urlsToRemove.size() > 0)
                    attrsToModify.put("-" + Provisioning.A_zimbraMtaAuthURL, urlsToRemove.toArray(new String[urlsToRemove.size()]));
            }

        } else if (mod.deleting()) {
            // delete all the zimbraMtaAuthURL values 
            attrsToModify.put(Provisioning.A_zimbraMtaAuthURL, null);
        }

    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
