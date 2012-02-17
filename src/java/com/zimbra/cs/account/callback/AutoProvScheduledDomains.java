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

import java.util.Map;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.AutoProvisionThread;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;

/**
 * Ensure domains scheduled for EAGER auto provision have EAGER auto provision 
 * mode enabled.
 */
public class AutoProvScheduledDomains extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName,
            Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        
        MultiValueMod mod = multiValueMod(attrsToModify, Provisioning.A_zimbraAutoProvScheduledDomains);
        if (mod != null && (mod.adding() || mod.replacing())) {
            for (String domainName : mod.valuesSet()) {
                Domain domain = prov.get(DomainBy.name, domainName);
                if (domain == null) {
                    throw AccountServiceException.NO_SUCH_DOMAIN(domainName);
                }
                
                if (!autoProvisionEnabled(domain)) {
                    throw ServiceException.INVALID_REQUEST(
                            "EAGER auto provision is not enabled on domain " + domainName, null);
                }
            }
        }
    }
    
    private boolean autoProvisionEnabled(Domain domain) {
        return domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode).contains(AutoProvMode.EAGER.name());
    }
    
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // do not run this callback unless inside the server
        if (!Zimbra.started()) {
            return;
        }
        
        try {
            if (entry instanceof Server) {
                // sanity check, this should not happen because ModifyServer is 
                // proxied to the the right server
                if (!((Server) entry).isLocalServer()) {
                    return;
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("unable to validate server", e);
            return;
        }
        
        try {
            AutoProvisionThread.switchAutoProvThreadIfNecessary();
        } catch (ServiceException e) {
            ZimbraLog.autoprov.error("unable to switch auto provisioning thread", e);
        }
    }


}
