
package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DomUtil;
import com.zimbra.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcCreateContactRequest extends LmcSoapRequest {

	private LmcContact mContact;
    
    /**
     * This method only sends the parameters from contact that the SOAP
     * protocol will accept.  That means folder ID, tags, and attributes.
     * Flags are currently ignored.
     * @param c - contact to create
     */
    public void setContact(LmcContact c) { mContact = c; }
    
    public LmcContact getContact() { return mContact; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(MailService.CREATE_CONTACT_REQUEST);
        Element newCN = DomUtil.add(request, MailService.E_CONTACT, "");
        LmcSoapRequest.addAttrNotNull(newCN, MailService.A_FOLDER, mContact.getFolder());
        LmcSoapRequest.addAttrNotNull(newCN, MailService.A_TAGS, mContact.getTags());
        
        // emit contact attributes if any
        LmcContactAttr attrs[] = mContact.getAttrs();
		for (int i = 0; attrs != null && i < attrs.length; i++)
			addContactAttr(newCN, attrs[i]);
		
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
		
        LmcCreateContactResponse response = new LmcCreateContactResponse();
        LmcContact c = parseContact(DomUtil.get(responseXML, MailService.E_CONTACT));
        response.setContact(c);
        return response;
	}

}
