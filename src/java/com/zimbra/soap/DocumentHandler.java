/*
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author schemers
 */
public abstract class DocumentHandler {
	public abstract Element handle(Element document, Map context) throws ServiceException;
	
	public ZimbraContext getZimbraContext(Map context) {
		return (ZimbraContext) context.get(SoapEngine.LIQUID_CONTEXT);
	}

    public Account getRequestedAccount(ZimbraContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();

        Account acct = Provisioning.getInstance().getAccountById(id);
        if (acct == null)
            throw ServiceException.AUTH_EXPIRED();
        return acct;
    }

    public Mailbox getRequestedMailbox(ZimbraContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();
        Mailbox mbx = Mailbox.getMailboxByAccountId(id);
        if (mbx != null)
            ZimbraLog.addToContext(mbx);
	    return mbx; 
    }

	/**
	 * by default, document handlers require a valid auth token
	 * @return
	 */
	public boolean needsAuth(Map context) {
		return true;
	}

	/**
	 * Should return true if this is an administrative command
	 * @return
	 */
	public boolean needsAdminAuth(Map context) {
		return false;
	}

    /**
     * Whether operation is read-only (true) or causes backend
     * state change (false).
     */
    public boolean isReadOnly() {
    	return true;
    }

    /**
     * Determines if client making the SOAP request is localhost.
     * @param context
     * @return
     */
    protected boolean clientIsLocal(Map context) {
        HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        if (req == null) return true;
        String peerIP = req.getRemoteAddr();
        return "127.0.0.1".equals(peerIP);
    }
}
