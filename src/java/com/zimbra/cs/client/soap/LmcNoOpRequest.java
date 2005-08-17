/*
 * Created on 2005. 3. 7.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.zimbra.soap.SoapParseException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LmcNoOpRequest extends LmcSoapRequest {

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.client.soap.LmcSoapRequest#getRequestXML()
	 */
	protected Element getRequestXML() throws LmcSoapClientException {
        Element request = DocumentHelper.createElement(MailService.NO_OP_REQUEST);
        return request;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.client.soap.LmcSoapRequest#parseResponseXML(org.dom4j.Element)
	 */
	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
        LmcNoOpResponse response = new LmcNoOpResponse();
        return response;
	}
}
