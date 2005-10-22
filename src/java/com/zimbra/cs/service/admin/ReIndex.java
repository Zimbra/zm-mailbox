package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;

public class ReIndex extends AdminDocumentHandler {
    
    private final String ACTION_START = "start";
    private final String ACTION_STATUS = "status";
    private final String ACTION_CANCEL = "cancel";

    private StopWatch sWatch = StopWatch.getInstance("ReIndex");
    
    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminService.E_MAILBOX, AdminService.A_ACCOUNTID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }
    
    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException {
        long startTime = sWatch.start();
        ZimbraContext zc = getZimbraContext(context);
        
        try {
            String action = request.getAttribute(MailService.E_ACTION);
            
            Element mreq = request.getElement(AdminService.E_MAILBOX);
            String accountId = mreq.getAttribute(AdminService.A_ACCOUNTID);
            
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
        } finally {
            sWatch.stop(startTime);
        }
        
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
