/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
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
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraMtaAuthHost + " is a single-valued attribute", null);
        
        // Set zimbraMtaAuthURL when zimbraMtaAuthHost is set
        String authHost = (String) value;
        String url = URLUtil.getMtaAuthURL(authHost);
        attrsToModify.put(Provisioning.A_zimbraMtaAuthURL, url);
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
