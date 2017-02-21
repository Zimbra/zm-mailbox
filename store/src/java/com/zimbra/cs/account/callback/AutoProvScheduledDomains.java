/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
