package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;

public class GetVCMeetingInfo extends AccountDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraLog.account.debug("GetVCMeetingInfo handle ---- ");
		ZimbraSoapContext zc = getZimbraSoapContext(context);

        Element response = getResponseElement(zc);
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("Please join my room for VC meeting.");strBuff.append("\n");
        strBuff.append("Join 101356 at intranicvc.nic.in (Room PIN: 46056)  using any of the following options:");strBuff.append("\n");
        strBuff.append("- To join as a guest user from your desktop or mobile device, Click http://intranicvc.nic.in/flex.html?roomdirect.html&amp;key=NT8RkYYH2k");strBuff.append("\n");
        strBuff.append(" - To join from a Studio based / H.323 endpoint  32101356");
        strBuff.append("\n");
        response.addAttribute("VCResponse", strBuff.toString());

		return response;
	}

}
