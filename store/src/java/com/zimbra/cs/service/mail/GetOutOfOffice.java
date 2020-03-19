package com.zimbra.cs.service.mail;

import java.util.Date;
import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class GetOutOfOffice extends MailDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		// TODO Auto-generated method stub
		Boolean isOutOfOffice = false;
		Element response = null;
		String uidParam = null;
        try {
        	ZimbraSoapContext zc = getZimbraSoapContext(context);

            response = getResponseElement(zc);

            uidParam = request.getAttribute(MailConstants.A_UID, null); 
	        if (uidParam != null) {
	    		Account acc = getAccountFromUid(uidParam);
	    		if(acc!=null) {
	    			isOutOfOffice = getoutOfOffice(acc);
	    		} else {
	    			ZimbraLog.account.debug("GetOutOfOffice account not found: "+uidParam);
	    		}
	    	}
	        ToXML.encodeOutOfOffice(response, uidParam, isOutOfOffice);
        } catch (Exception e) {
    		ZimbraLog.account.error("GetOutOfOffice failed", e);
    	}
        ZimbraLog.account.debug("GetOutOfOffice account: "+uidParam+", isOutOfOffice: "+isOutOfOffice);
        
		return response;
	}
	
	private Account getAccountFromUid(String uid) {
    	Provisioning prov = Provisioning.getInstance();
    	Account acct = null;
    	try {
    		if (Provisioning.isUUID(uid))
    			acct = prov.get(AccountBy.id, uid);
    		else
    			acct = prov.get(AccountBy.name, uid);
    		ZimbraLog.account.debug("GetOutOfOffice account: "+acct);
    	} catch (Exception e) {
    		ZimbraLog.account.error("GetOutOfOffice failed during get account", e);
    		acct = null;
    	}
    	return acct;
    }
	
	private boolean getoutOfOffice(Account account) {
		try {
	        boolean replyEnabled = account.isPrefOutOfOfficeReplyEnabled();
	        if (!replyEnabled || account.isAccountExternal()) {
	            return false;
	        }
	        
	        // Check if we are in any configured out of office interval zimbraFileUploadMaxSize
	        Date now = new Date();
	        Date fromDate = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeFromDate, null);
	        if (fromDate != null && now.before(fromDate)) {
	            return false;
	        }
	
	        Date untilDate = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, null);
	        if (untilDate != null && now.after(untilDate)) {
	            return false;
	        }
	        
	        ZimbraLog.account.debug("GetOutOfOffice fromDate: "+fromDate+", untilDate: "+untilDate);
	        if(fromDate!=null && untilDate!=null && (!now.before(fromDate) && !now.after(untilDate))) {
	        	return true;
	        } else {
	        	return false;
	        }
		} catch (Exception e) {
    		ZimbraLog.account.error("GetOutOfOffice getoutOfOffice failed", e);
    		return false;
    	}
    }

}
