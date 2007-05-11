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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.admin.AdminServiceException;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.session.IWaitSet;
import com.zimbra.cs.session.WaitSetAccount;
import com.zimbra.cs.session.WaitSetCallback;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class WaitSetRequest extends MailDocumentHandler {
    
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
        String waitSetId = request.getAttribute(MailConstants.A_WAITSET_ID);
        String lastKnownSeqNo = request.getAttribute(MailConstants.A_SEQ);
        boolean block = request.getAttributeBool(MailConstants.A_BLOCK, false);
        
        boolean adminAllowed = zsc.getAuthToken().isAdmin(); 
        
        String defInterestStr = null;
        IWaitSet ws = null;
        
        if (waitSetId.startsWith(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX)) {
            
            if (!adminAllowed) 
                throw MailServiceException.PERM_DENIED("Non-Admin accounts may not wait on other accounts");
            
            // default interest types required for "All" waitsets
            defInterestStr = request.getAttribute(MailConstants.A_DEFTYPES);
            int defaultInterests = WaitSetRequest.parseInterestStr(defInterestStr, 0);
            ws = WaitSetMgr.lookupOrCreateForAllAccts(zsc.getAuthtokenAccountId(), waitSetId, defaultInterests, lastKnownSeqNo);
        } else {
            ws = WaitSetMgr.lookup(waitSetId);
        }
        
        if (ws == null) {
            throw AdminServiceException.NO_SUCH_WAITSET(waitSetId);
        }
        
        if (!ws.getOwnerAccountId().equals(zsc.getAuthtokenAccountId())) {
            throw ServiceException.PERM_DENIED("Not owner of waitset");
        }
        
        List<WaitSetAccount> add = parseAddUpdateAccounts(
            request.getOptionalElement(MailConstants.E_WAITSET_ADD), ws.getDefaultInterest());
        List<WaitSetAccount> update = parseAddUpdateAccounts(
            request.getOptionalElement(MailConstants.E_WAITSET_UPDATE), ws.getDefaultInterest());
        List<String> remove = parseRemoveAccounts(request.getOptionalElement(MailConstants.E_WAITSET_REMOVE));
        
        if (!adminAllowed) {
            for (WaitSetAccount wsa : add) {
                if (!wsa.accountId.equals(zsc.getAuthtokenAccountId()))
                    throw MailServiceException.PERM_DENIED("Non-Admin accounts may not wait on other accounts");
            }
            for (WaitSetAccount wsa : update) {
                if (!wsa.accountId.equals(zsc.getAuthtokenAccountId()))
                    throw MailServiceException.PERM_DENIED("Non-Admin accounts may not wait on other accounts");
            }
            for (String acctId : remove) {
                if (!acctId.equals(zsc.getAuthtokenAccountId()))
                    throw MailServiceException.PERM_DENIED("Non-Admin accounts may not wait on other accounts");
            }
        }
        
        Callback cb = new Callback();

        List<WaitSetError> errors  = null;

        // Force the client to wait briefly before processing -- this will stop 'bad' clients from polling 
        // the server in a very fast loop (they should be using the 'block' mode)
        try { Thread.sleep(1000); } catch (InterruptedException ex) {} 
        
        synchronized(cb) {
            errors = ws.doWait(cb, lastKnownSeqNo, block, add, update, remove);
            
            if (block && !cb.completed) { // don't wait if it completed right away
                
                // No data after initial check...wait a few extra seconds
                // before going into the notification wait...basically we're just 
                // trying to let the server coalesce notification data a little 
                // bit.
                try { Thread.sleep(3000); } catch (InterruptedException ex) {} 

                try { 
                    cb.wait(1000 * 60 * 5); // timeout after 5 minutes
                } catch (InterruptedException ex) {}
                
            }
        }
        
        Element response = zsc.createElement(MailConstants.WAIT_SET_RESPONSE);
        response.addAttribute(MailConstants.A_WAITSET_ID, waitSetId);
        if (cb.canceled) {
            response.addAttribute(MailConstants.A_CANCELED, true);
        } else if (cb.completed) {
            response.addAttribute(MailConstants.A_SEQ, cb.seqNo);
            
            for (String s : cb.signalledAccounts) {
                Element saElt = response.addElement(MailConstants.E_A);
                saElt.addAttribute(MailConstants.A_ID, s);
            }
        } else {
            // timed out....they should try again
            response.addAttribute(MailConstants.A_SEQ, lastKnownSeqNo);
        }
        
        encodeErrors(response, errors);
        
        return response;
    }
    
    static List<WaitSetAccount> parseAddUpdateAccounts(Element elt, int defaultInterest) throws ServiceException {
        List<WaitSetAccount> toRet = new ArrayList<WaitSetAccount>();
        if (elt != null) {
            for (Iterator<Element> iter = elt.elementIterator(MailConstants.E_A); iter.hasNext();) {
                Element a = iter.next();
                String id = a.getAttribute(MailConstants.A_ID);
                String tokenStr = a.getAttribute(MailConstants.A_TOKEN, null);
                SyncToken token = tokenStr != null ? new SyncToken(tokenStr) : null;
                int interests = parseInterestStr(a.getAttribute(MailConstants.A_TYPES, null), defaultInterest);
                toRet.add(new WaitSetAccount(id, token, interests));
            }
        }
        return toRet;
    }
    
    static List<String> parseRemoveAccounts(Element elt) throws ServiceException {
        List<String> remove = new ArrayList<String>();
        if (elt != null) {
            for (Iterator<Element> iter = elt.elementIterator(MailConstants.E_A); iter.hasNext();) {
                Element a = iter.next();
                String id = a.getAttribute(MailConstants.A_ID);
                remove.add(id);
            }
        }
        return remove;
    }
    
    public static class Callback implements WaitSetCallback {
        public void dataReady(IWaitSet ws, String seqNo, boolean canceled, String[] signalledAccounts) {
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
        public IWaitSet waitSet;
        public String seqNo;
    }
    
    public static enum TypeEnum {
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
            Element errorElt = parent.addElement(MailConstants.E_ERROR);
            errorElt.addAttribute(MailConstants.A_ID, error.accountId); 
            errorElt.addAttribute(MailConstants.A_TYPE, error.error.name()); 
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
