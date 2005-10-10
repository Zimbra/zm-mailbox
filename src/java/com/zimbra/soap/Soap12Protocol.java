/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Soap12Protocol.java
 */

package com.zimbra.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.SoapFaultException;

/**
 * Interface to Soap 1.2 Protocol
 */

class Soap12Protocol extends SoapProtocol {

    private static final String NS_STR = "http://www.w3.org/2003/05/soap-envelope";
    private static final Namespace NS = Namespace.get(NS_PREFIX, NS_STR);
    private static final QName CODE = QName.get("Code", NS);
    private static final QName REASON = QName.get("Reason", NS);
    private static final QName TEXT = QName.get("Text", NS);
    private static final QName DETAIL = QName.get("Detail", NS);
    private static final QName VALUE = QName.get("Value", NS);
    private static final QName SENDER_CODE = QName.get("Sender", NS);
    private static final QName RECEIVER_CODE = QName.get("Receiver", NS);

    /** empty package-private constructor */
    Soap12Protocol() { 
        super();
    }

    public Element.ElementFactory getFactory() {
        return Element.XMLElement.mFactory;
    }

    /**
     * Return the namespace String
     */
    public Namespace getNamespace()
    {
        return NS;
    }

    /** 
     * Given an Element that represents a fault (i.e,. isFault returns 
     * true on it), construct a SoapFaultException from it. 
     *
     * @return new SoapFaultException
     * @throws ServiceException
     */
    public SoapFaultException soapFault(Element fault)
    {
    	if (!isFault(fault))
    		return new SoapFaultException("not a soap fault ", fault);
    	
    	Element code = fault.getOptionalElement(CODE);
        boolean isReceiversFault = RECEIVER_CODE.equals(code == null ? null : code.getQName());

    	String reasonValue;
    	Element reason = fault.getOptionalElement(REASON);
        Element reasonText = (reason == null ? null : reason.getOptionalElement(TEXT));
    	if (reasonText != null)
    		reasonValue = reasonText.getTextTrim();
        else
    		reasonValue = "unknown reason";

    	Element detail = fault.getOptionalElement(DETAIL);

        return new SoapFaultException(reasonValue, detail, isReceiversFault, fault);
    }

    /** 
     * Given a ServiceException, wrap it in a soap fault return the 
     * soap fault document.
     */
    public Element soapFault(ServiceException e)
    {
        String reason = e.getMessage();
        if (reason == null)
            reason = e.toString();
        QName code;
        
        if (e.isReceiversFault())
            code = RECEIVER_CODE;
        else 
            code = SENDER_CODE;

        Element eFault = mFactory.createElement(mFaultQName);
        Element eCode = eFault.addUniqueElement(CODE);
        // FIXME: should really be a qualified "attribute"
        eCode.addUniqueElement(VALUE).setText(code.getQualifiedName());
        Element eReason = eFault.addUniqueElement(REASON);
        // FIXME: should really be a qualified "attribute"
        eReason.addUniqueElement(TEXT).setText(reason);
        Element eDetail = eFault.addUniqueElement(DETAIL);
        Element error = eDetail.addUniqueElement(ZimbraNamespace.E_ERROR);
        // FIXME: should really be a qualified "attribute"
        error.addUniqueElement(ZimbraNamespace.E_CODE).setText(e.getCode());
        return eFault;
    }

    /** Return Content-Type header */
    public String getContentType() {
        // should be using application/soap+xml, but Safari croaks
        return "text/xml; charset=utf-8";
        //return "application/soap+xml; charset=utf-8";
    }

    /** Whether or not to include a HTTP SOAPActionHeader. */
    public boolean hasSOAPActionHeader() {
        return false;
    }

    public String getVersion() {
        return "1.2.";
    }
}
