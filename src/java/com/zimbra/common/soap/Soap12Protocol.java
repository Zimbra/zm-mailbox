/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Soap12Protocol.java
 */

package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.soap.SoapFaultException;

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

    /** Return the namespace String */
    public Namespace getNamespace() {
        return NS;
    }

    /** Given an Element that represents a fault (i.e,. isFault returns 
     *  true on it), construct a SoapFaultException from it. 
     *
     * @return new SoapFaultException
     * @throws ServiceException */
    public SoapFaultException soapFault(Element fault) {
    	if (!isFault(fault))
    		return new SoapFaultException("not a soap fault ", fault);
    	
    	boolean isReceiversFault = false;
    	Element code = fault.getOptionalElement(CODE);
    	if (code != null) {
    		Element value = code.getOptionalElement(VALUE);
    		isReceiversFault = RECEIVER_CODE.getQualifiedName().equals(value == null ? null : value.getTextTrim());
    	}

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
    public Element soapFault(ServiceException e) {
        String reason = e.getMessage();
        if (reason == null)
            reason = e.toString();
        
        QName code = e.isReceiversFault() ? RECEIVER_CODE : SENDER_CODE;

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
        error.addUniqueElement(ZimbraNamespace.E_TRACE).setText(e.getId());
        
        if (e.getArgs() != null) {
            for (ServiceException.Argument arg : e.getArgs()) {
                Element val = error.addElement(ZimbraNamespace.E_ARGUMENT);
                val.addAttribute(ZimbraNamespace.A_ARG_NAME, arg.mName);
                val.addAttribute(ZimbraNamespace.A_ARG_TYPE, arg.mType.toString());
                val.setText(arg.mValue);
            }
        }
        
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
