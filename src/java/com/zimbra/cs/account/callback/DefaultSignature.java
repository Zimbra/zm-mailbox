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

package com.zimbra.cs.account.callback;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
 
public class DefaultSignature implements AttributeCallback {

    /**
     * check to make sure signature is one of the signatures of the account
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraPrefDefaultSignature + " is a single-valued attribute", null);
        
        String defaultSigName = (String)value;
        
        if (entry == null) {
            // we are creating an accout
            String acctSigName = (String)attrsToModify.get(Provisioning.A_zimbraPrefSignatureName);
            if (acctSigName == null || !acctSigName.equals(defaultSigName))
                throw ServiceException.INVALID_REQUEST("cannot set " + Provisioning.A_zimbraPrefDefaultSignature + " to a non-existing signature name", null);
        } else {
            if (entry instanceof Account) {
                List<Signature> sigs = Provisioning.getInstance().getAllSignatures((Account)entry);
                for (Signature sig : sigs) {
                    if (sig.getName().equals(defaultSigName))
                        return;
                }
                
                /*
                 * it could be we are creating a signature and setting it to default because there is
                 * currently no default.  The only situation for that is we are creating a signature 
                 * on the account entry, for that we can check if the signature name being created 
                 * is the same as the default signature.
                 */  
                String acctSigName = (String)attrsToModify.get(Provisioning.A_zimbraPrefSignatureName);
                if (acctSigName == null || !acctSigName.equals(defaultSigName))
                    throw ServiceException.INVALID_REQUEST("cannot set " + Provisioning.A_zimbraPrefDefaultSignature + " to a non-existing signature name", null);
            }
        }
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
