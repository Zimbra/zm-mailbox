/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.session.WaitSetAccount;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class CreateWaitSet extends MailDocumentHandler {
    /*
     <!--*************************************
          CreateWaitSet: must be called once to initialize the WaitSet
          and to set its "default interest types"
         ************************************* -->
        <CreateWaitSetRequest defTypes="DEFAULT_INTEREST_TYPES" [all="1"]>
          [ <add>
            [<a id="ACCTID" [token="lastKnownSyncToken"] [types="if_not_default"]/>]+
            </add> ]
        </CreateWaitSetRequest>

        <CreateWaitSetResponse waitSet="setId" defTypes="types" seq="0">
          [ <error ...something.../>]*
        </CreateWaitSetResponse>  
     */

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(MailConstants.CREATE_WAIT_SET_RESPONSE);
        return staticHandle(request, context, response);
    }
    
    static public Element staticHandle(Element request, Map<String, Object> context, Element response) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        String defInterestStr = request.getAttribute(MailConstants.A_DEFTYPES);
        int defaultInterests = WaitSetRequest.parseInterestStr(defInterestStr, 0);
        boolean adminAllowed = zsc.getAuthToken().isAdmin();
        
        boolean allAccts = request.getAttributeBool(MailConstants.A_ALL_ACCOUNTS, false);
        if (allAccts && !adminAllowed) {
            throw MailServiceException.PERM_DENIED("Non-Admin accounts may not wait on other accounts");
        }
        
        List<String> allowedAccountIds = null;
        if (!adminAllowed) {
            allowedAccountIds = new ArrayList<String>(1);
            allowedAccountIds.add(zsc.getAuthtokenAccountId());
        }
        
        List<WaitSetAccount> add = WaitSetRequest.parseAddUpdateAccounts(
            request.getOptionalElement(MailConstants.E_WAITSET_ADD), defaultInterests, allowedAccountIds);
        
        // workaround for 27480: load the mailboxes NOW, before we grab the waitset lock
        List<Mailbox> referencedMailboxes = new ArrayList<Mailbox>();
        for (WaitSetAccount acct : add) {
            try {
                MailboxManager.FetchMode fetchMode = MailboxManager.FetchMode.AUTOCREATE;
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getAccountId(), fetchMode);
                referencedMailboxes.add(mbox);
            } catch (ServiceException e) {
                ZimbraLog.session.debug("Caught exception preloading mailbox for waitset", e);
            }
        }
        

        Pair<String, List<WaitSetError>> result = WaitSetMgr.create(zsc.getAuthtokenAccountId(), adminAllowed, defaultInterests, allAccts, add);
        String wsId = result.getFirst();
        List<WaitSetError> errors = result.getSecond();
        
        response.addAttribute(MailConstants.A_WAITSET_ID, wsId);
        response.addAttribute(MailConstants.A_DEFTYPES, WaitSetRequest.interestToStr(defaultInterests));
        response.addAttribute(MailConstants.A_SEQ, 0);
        
        WaitSetRequest.encodeErrors(response, errors);
        
        return response;
    }

}
