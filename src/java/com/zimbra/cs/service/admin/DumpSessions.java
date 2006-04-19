package com.zimbra.cs.service.admin;

import java.io.StringWriter;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class DumpSessions extends AdminDocumentHandler {

	@Override
	public Element handle(Element request, Map context) throws ServiceException, SoapFaultException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		Element response = lc.createElement(AdminService.DUMP_SESSIONS_RESPONSE);
		
		StringWriter sw = new StringWriter();
		SessionCache.dumpState(sw);
		response.setText(sw.toString());
		
		return response;
	}

}
