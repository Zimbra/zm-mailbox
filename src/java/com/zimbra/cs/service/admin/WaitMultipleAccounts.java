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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.session.WaitSet;
import com.zimbra.cs.session.WaitSet.WaitSetAccount;
import com.zimbra.cs.session.WaitSet.WaitSetError;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class WaitMultipleAccounts extends AdminDocumentHandler {
    
    /*
<!--*************************************
    WaitMultipleAccounts:  optionally modifies the wait set and checks
    for any notifications.  If block=1 and there are no notificatins, then
    this API will BLOCK until there is data.

    Client should always set 'seq' to be the highest known value it has
    received from the server.  The server will use this information to
    retransmit lost data.

    If the client sends a last known sync token then the notification is
    calculated by comparing the accounts current token with the client's
    last known.

    If the client does not send a last known sync token, then notification
    is based on change since last Wait (or change since <add> if this
    is the first time Wait has been called with the account)
    ************************************* -->
<WaitMultipleAccountsRequest waitSet="setId" seq="highestSeqKnown" [block="1"]>
  [ <add>
      [<a id="ACCTID" [token="lastKnownSyncToken"] [types="a,c..."]/>]+
    </add> ]
  [ <update>
      [<a id="ACCTID" [token="lastKnownSyncToken"] [types=]/>]+
    </update> ]  
  [ <remove>
      [<a id="ACCTID"/>]+
    </remove> ]  
</WaitMultipleAccountsRequest>

<WaitMultipleAccountsResponse waitSet="setId" seq="seqNo" [canceled="1"]>
  [ <n id="ACCTID"/>]*
  [ <error ...something.../>]*
</WaitMultipleAccountsResponse>
     */

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String waitSetId = request.getAttribute(AdminConstants.A_WAITSET_ID);
        int lastKnownSeqNo = (int)request.getAttributeLong(AdminConstants.A_SEQ);
        boolean block = request.getAttributeBool(AdminConstants.A_BLOCK, false);
        
        WaitSet ws = WaitSet.lookup(waitSetId);

        if (ws == null) {
            throw AdminServiceException.NO_SUCH_WAITSET(waitSetId);
        }
        
        List<WaitSetAccount> add = parseAddUpdateAccounts(
            request.getOptionalElement(AdminConstants.E_WAITSET_ADD), ws.getDefaultInterest());
        List<WaitSetAccount> update = parseAddUpdateAccounts(
            request.getOptionalElement(AdminConstants.E_WAITSET_UPDATE), ws.getDefaultInterest());
        List<String> remove = parseRemoveAccounts(request.getOptionalElement(AdminConstants.E_WAITSET_REMOVE));
        
        Callback cb = new Callback();

        List<WaitSetError> errors  = null;        
        synchronized(cb) {
            errors = ws.doWait(cb, lastKnownSeqNo, block, add, update, remove);
            
            if (block && !cb.completed) { // don't wait if it completed right away
                try {
                    cb.wait(1000 * 60 * 5); // timeout after 5 minutes
                } catch (InterruptedException ex) {} 
            }
        }
        
        Element response = zsc.createElement(AdminConstants.WAIT_MULTIPLE_ACCOUNTS_RESPONSE);
        response.addAttribute(AdminConstants.A_WAITSET_ID, waitSetId);
        if (cb.canceled) {
            response.addAttribute(AdminConstants.A_CANCELED, true);
        } else if (cb.completed) {
            response.addAttribute(AdminConstants.A_SEQ, cb.seqNo);
            
            for (String s : cb.signalledAccounts) {
                Element saElt = response.addElement(AdminConstants.E_A);
                saElt.addAttribute(AdminConstants.A_ID, s);
            }
        } else {
            // timed out....they should try again
            response.addAttribute(AdminConstants.A_SEQ, lastKnownSeqNo);
        }
        
        encodeErrors(response, errors);
        
        return response;
    }
    
    static List<WaitSetAccount> parseAddUpdateAccounts(Element elt, int defaultInterest) throws ServiceException {
        List<WaitSetAccount> toRet = new ArrayList<WaitSetAccount>();
        if (elt != null) {
            for (Iterator<Element> iter = elt.elementIterator(AdminConstants.E_A); iter.hasNext();) {
                Element a = iter.next();
                String id = a.getAttribute(AdminConstants.A_ID);
                String tokenStr = a.getAttribute(AdminConstants.A_TOKEN, null);
                SyncToken token = tokenStr != null ? new SyncToken(tokenStr) : null;
                int interests = parseInterestStr(a.getAttribute(AdminConstants.A_TYPES, null), defaultInterest);
                toRet.add(new WaitSetAccount(id, token, interests));
            }
        }
        return toRet;
    }
    
    static List<String> parseRemoveAccounts(Element elt) throws ServiceException {
        List<String> remove = new ArrayList<String>();
        if (elt != null) {
            for (Iterator<Element> iter = elt.elementIterator(AdminConstants.E_A); iter.hasNext();) {
                Element a = iter.next();
                String id = a.getAttribute(AdminConstants.A_ID);
                remove.add(id);
            }
        }
        return remove;
    }
    
    static class Callback implements WaitSet.WaitSetCallback {
        public void dataReady(WaitSet ws, int seqNo, boolean canceled, String[] signalledAccounts) {
            synchronized(this) {
                ZimbraLog.session.info("Called WaitMultiplAccounts WaitSetCallback.dataReady()!");
                this.waitSet = ws;
                this.canceled = canceled;
                this.signalledAccounts = signalledAccounts;
                this.seqNo = seqNo;
                this.completed = true;
                this.notifyAll();
            }
        }

        public boolean completed = false;
        public boolean canceled;
        public String[] signalledAccounts;
        public WaitSet waitSet;
        public int seqNo;
    }
    
    static enum TypeEnum {
        m(MailItem.typeToBitmask(MailItem.TYPE_MESSAGE)),
        c(MailItem.typeToBitmask(MailItem.TYPE_CONTACT)),
        a(MailItem.typeToBitmask(MailItem.TYPE_APPOINTMENT)),
        all(0xffffff),
        ;
        
        TypeEnum(int mask) { mMask = mask; }
        public int getMask() { return mMask; }
        
        private final int mMask;
    }
    
    public static final void encodeErrors(Element parent, List<WaitSetError> errors) {
        for (WaitSetError error : errors) {
            Element errorElt = parent.addElement(AdminConstants.E_ERROR);
            errorElt.addAttribute(AdminConstants.A_ID, error.accountId); 
            errorElt.addAttribute(AdminConstants.A_TYPE, error.error.name()); 
        }
    }
    
    public static final int parseInterestStr(String typesList, int defaultInterest) {
        if (typesList == null)
            return defaultInterest;
        
        int toRet = 0;
        
        for (String s : typesList.split(",")) {
            s = s.trim();
            TypeEnum te = TypeEnum.valueOf(s);
            toRet |= te.getMask();
        }
        return toRet;
    }
    
    public static final String interestToStr(int interests) {
        StringBuilder toRet = new StringBuilder();
        
        boolean atFirst = true;
        for (TypeEnum t : TypeEnum.values()) {
            if (t != TypeEnum.all && (interests & t.getMask())!=0) {
                if (!atFirst) {
                    toRet.append(',');
                }
                atFirst = false;
                toRet.append(t.name());
            }
        }
        return toRet.toString();
    }

}
