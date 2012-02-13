/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.AutoProvisionThread;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;

public class AutoProvMode extends AttributeCallback {
    
    @Override
    public void preModify(CallbackContext context, String attrName,
            Object attrValue, Map attrsToModify, Entry entry)
    throws ServiceException {
        
        assert(Provisioning.A_zimbraAutoProvMode.equalsIgnoreCase(attrName));
        
        if (entry == null || context.isCreate() || !(entry instanceof Domain)) {
            return;
        }
        
        Domain domain = (Domain) entry;
        Set<String> curModes = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode);
        
        MultiValueMod mod = multiValueMod(attrsToModify, attrName);
        Set<String> newModes = newValuesToBe(mod, entry, attrName);
        
        boolean removingEagerMode = 
                curModes.contains(Provisioning.AutoProvMode.EAGER.name()) &&
                !newModes.contains(Provisioning.AutoProvMode.EAGER.name());

        if (removingEagerMode) {
            /*
             * remove the domain from scheduled domains on all servers
             * 
             * the changes are made to LDAP, but only Server instances on this node is
             * refreshed.  Server instances on other nodes will be updated after the 
             * normal 15 minutes cache period.
             * 
             * do *not* trigger the callback for zimbraAutoProvScheduledDomains when 
             * we modify server.  It is cleaner to handle the stopping auto prov thread 
             * separately in the postModify of this callback, instead of the postModify 
             * method in the callback for for zimbraAutoProvScheduledDomains) 
             */
            Provisioning prov = Provisioning.getInstance();
            for (Server server : Provisioning.getInstance().getAllServers()) {
                Set<String> scheduledDomains = server.getMultiAttrSet(Provisioning.A_zimbraAutoProvScheduledDomains);
                if (scheduledDomains.contains(domain.getName())) {
                    HashMap<String,Object> attrs = new HashMap<String,Object>();
                    StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAutoProvScheduledDomains, domain.getName());
                    prov.modifyAttrs(server, attrs, false, false); // do not allowCallback
                }
            }
        }
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // do not run this callback unless inside the server
        if (!Zimbra.started()) {
            return;
        }

        try {
            AutoProvisionThread.switchAutoProvThreadIfNecessary();
        } catch (ServiceException e) {
            ZimbraLog.autoprov.error("unable to switch auto provisioning thread", e);
        }
    }


}
