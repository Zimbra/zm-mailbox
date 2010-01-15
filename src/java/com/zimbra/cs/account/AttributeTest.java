/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;

public class AttributeTest {
    public static void main(String args[]) throws ServiceException {
        CliUtil.toolSetup("INFO");
        AttributeManager mgr = AttributeManager.getInstance();
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        attrs.put(Provisioning.A_zimbraImapBindPort, "143");
        attrs.put("xxxzimbraImapBindPort", "143");
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, null);
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, "FALSE");
        Map context = new HashMap();
        mgr.preModify(attrs, null, context, false, true);
        // modify
        mgr.postModify(attrs, null, context, false);
    }
}
