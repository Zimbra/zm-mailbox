package com.zimbra.cs.operation;

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.RemoteServerRequest;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.ArrayUtil;

public class CheckSpellingOperation extends Operation {
	
	private static final int LOAD = 10;
	
	private String[] mUrls;
	private String mText;
	private Map mResult;
	
	public CheckSpellingOperation(Session session, OperationContext oc,
				Mailbox mbox, Requester req, String text) throws ServiceException {
		super(session, oc, mbox, req, LOAD);
		
		// Make sure that the spell server URL is specified
		Provisioning prov = Provisioning.getInstance();
		Server localServer = prov.getLocalServer();
		mUrls = localServer.getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
		if (ArrayUtil.isEmpty(mUrls)) {
			String errorStr = Provisioning.A_zimbraSpellCheckURL + " is not specified";
			getLog().info(toString()+": "+ errorStr);
			throw ServiceException.NO_SPELL_CHECK_URL(errorStr);
		}
		
		mText = text;
		
		run();
	}

	protected void callback() throws ServiceException {
		for (int i = 0; i < mUrls.length; i++) {
			RemoteServerRequest req = new RemoteServerRequest();
			req.addParameter("text", mText);
			String url = mUrls[i];
			try {
				if (getLog().isDebugEnabled())
					getLog().debug("CheckSpellingOperation: Attempting to check spelling at " + url);
				req.invoke(url);
				mResult = req.getResponseParameters();
				break; // Successful request.  No need to check the other servers.
			} catch (IOException ex) {
				getLog().warn(toString()+": An error occurred while contacting " + url, ex);
			}
		}
	}
	
	public Map getResult() { return mResult; } 
}
