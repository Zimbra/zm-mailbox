/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

public class AttributeTest {
    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup("INFO");
        AttributeManager mgr = AttributeManager.getInstance();
        HashMap attrs = new HashMap();
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
