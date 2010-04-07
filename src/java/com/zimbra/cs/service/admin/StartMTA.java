package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.soap.ZimbraSoapContext;

public class StartMTA extends AdminDocumentHandler {
	@Override
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		
	    Element serverEl = request.getElement(AdminConstants.E_SERVER);
	    String method = serverEl.getAttribute(AdminConstants.A_BY);
	    String serverName = serverEl.getText();
	    Provisioning prov = Provisioning.getInstance();
		Server server = prov.get(ServerBy.fromString(method), serverName);
		if (server == null) {
			throw ServiceException.INVALID_REQUEST("Cannot find server record for the host: " + serverName, null);
		}		
		checkRight(zsc, context, server, Admin.R_startMTA);
		RemoteManager rmgr = RemoteManager.getRemoteManager(server);
		RemoteResult rr = rmgr.execute(RemoteCommands.START_MTA);
		Element response = zsc.createElement(AdminConstants.START_MTA_RESPONSE);
		return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_startMTA);
        notes.add("The right has to be granted on the target server that RemoteManager will send the command to");
    }
}
