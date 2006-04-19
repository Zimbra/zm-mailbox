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

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ReIndex extends AdminDocumentHandler {
    
    private final String ACTION_START = "start";
    private final String ACTION_STATUS = "status";
    private final String ACTION_CANCEL = "cancel";

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminService.E_MAILBOX, AdminService.A_ACCOUNTID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }
    
    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext zc = getZimbraContext(context);

        String action = request.getAttribute(MailService.E_ACTION);
        
        Element mreq = request.getElement(AdminService.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminService.A_ACCOUNTID);
        
        Account account = Provisioning.getInstance().getAccountById(accountId);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        
        if (!canAccessAccount(zc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        Mailbox mbox = Mailbox.getMailboxByAccountId(accountId, false);
        if (mbox == null)
            throw ServiceException.FAILURE("mailbox not found for account " + accountId, null);
        
        Element response = zc.createElement(AdminService.REINDEX_RESPONSE);
        
        if (action.equalsIgnoreCase(ACTION_START)) {
            if (mbox.isReIndexInProgress()) {
                throw ServiceException.ALREADY_IN_PROGRESS(accountId, "ReIndex");
            }
            
            ReIndexThread thread = new ReIndexThread(mbox);
            thread.start();
            
            response.addAttribute(AdminService.A_STATUS, "started");
        } else if (action.equalsIgnoreCase(ACTION_STATUS)) {
            synchronized (mbox) {
                if (!mbox.isReIndexInProgress()) {
                    throw ServiceException.NOT_IN_PROGRESS(accountId, "ReIndex");
                }
                
                Mailbox.ReIndexStatus status = mbox.getReIndexStatus();
                
                addStatus(response, status);
            }
            response.addAttribute(AdminService.A_STATUS, "running");
            
        } else if (action.equalsIgnoreCase(ACTION_CANCEL)) {
            synchronized (mbox) {
                if (!mbox.isReIndexInProgress()) {
                    throw ServiceException.NOT_IN_PROGRESS(accountId, "ReIndex");
                }
                
                Mailbox.ReIndexStatus status = mbox.getReIndexStatus();
                status.mCancel = true;
                
                response.addAttribute(AdminService.A_STATUS, "cancelled");
                addStatus(response, status);
            }
        } else {
            throw ServiceException.INVALID_REQUEST("Unknown action: "+action, null);
        }
        
        return response;
    }
    
    public static void addStatus(Element response, Mailbox.ReIndexStatus status) {
        Element prog = response.addElement(AdminService.E_PROGRESS);
        prog.addAttribute(AdminService.A_NUM_SUCCEEDED, (status.mNumProcessed-status.mNumFailed));
        prog.addAttribute(AdminService.A_NUM_FAILED, status.mNumFailed);
        prog.addAttribute(AdminService.A_NUM_REMAINING, (status.mNumToProcess-status.mNumProcessed));
    }
    
    public static class ReIndexThread extends Thread
    {
        private Mailbox mMbox;
        
        ReIndexThread(Mailbox mbox) {
            mMbox = mbox;
        }
            
        public void run() {
            try {
                mMbox.reIndex();
            } catch (ServiceException e) {
                if (!e.getCode().equals(ServiceException.INTERRUPTED)) 
                    e.printStackTrace();
            }
        }
    }

}
