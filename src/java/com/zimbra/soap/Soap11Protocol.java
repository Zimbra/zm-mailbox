/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
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
 * Soap11Protocol.java
 */

package com.zimbra.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ExceptionToString;

/**
 * Interface to Soap 1.1 Protocol
 */

class Soap11Protocol extends SoapProtocol {

    private static final String NS_STR =
        "http://schemas.xmlsoap.org/soap/envelope/";
    private static final Namespace NS = Namespace.get(NS_PREFIX, NS_STR);
    private static final QName FAULTCODE = new QName("faultcode", NS);
    private static final QName FAULTSTRING = new QName("faultstring", NS);
    private static final QName DETAIL = new QName("detail", NS);
    private static final QName SENDER_CODE = new QName("Client", NS);
    private static final QName RECEIVER_CODE = new QName("Server", NS);

    /** empty package-private constructor */
    Soap11Protocol() { 
        super();
    }

    public Element.ElementFactory getFactory() {
        return Element.XMLElement.mFactory;
    }

    /**
     * Return the namespace String
     */
    public Namespace getNamespace() {
        return NS;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.shared.SoapProtocol#soapFault(org.dom4j.Element)
     */
    public SoapFaultException soapFault(Element fault) {
        if (!isFault(fault))
            return new SoapFaultException("not a soap fault ", fault);
        
        Element code = fault.getOptionalElement(FAULTCODE);
        boolean isReceiversFault = RECEIVER_CODE.equals(code == null ? null : code.getQName());

        String reasonValue;
        Element faultString = fault.getOptionalElement(FAULTSTRING);
        if (faultString != null)
            reasonValue = faultString.getTextTrim();
        else
            reasonValue = "unknown reason";

        Element detail = fault.getOptionalElement(DETAIL);

        return new SoapFaultException(reasonValue, detail, isReceiversFault, fault);
    }

    /* (non-Javadoc)
     * @see com.zimbra.soap.SoapProtocol#soapFault(com.zimbra.cs.service.ServiceException)
     */
    public Element soapFault(ServiceException e) {
        String reason = e.getMessage();
        if (reason == null)
            reason = e.toString();
        QName code;
        
        if (e.isReceiversFault())
            code = RECEIVER_CODE;
        else 
            code = SENDER_CODE;

        Element eFault = mFactory.createElement(mFaultQName);
        eFault.addUniqueElement(FAULTCODE).setText(code.getQualifiedName());
        eFault.addUniqueElement(FAULTSTRING).setText(reason);
        Element eDetail = eFault.addUniqueElement(DETAIL);
        Element error = eDetail.addUniqueElement(ZimbraNamespace.E_ERROR);
        // FIXME: should really be a qualified "attribute"
        error.addUniqueElement(ZimbraNamespace.E_CODE).setText(e.getCode());
        error.addUniqueElement(ZimbraNamespace.E_TRACE).setText(ExceptionToString.ToString(e));
        
        if (e.getArgs() != null) {
            for (ServiceException.Argument arg : e.getArgs()) {
                Element val = error.addElement("a");
                val.addAttribute("n", arg.mName);
                val.setText(arg.mValue);
            }
        }
        
        return eFault;
    }

    /** Return Content-Type header */
    public String getContentType() {
        return "text/xml; charset=utf-8";
    }

    /** Whether or not to include a SOAPActionHeader */
    public boolean hasSOAPActionHeader() {
        return true;
    }

    public String getVersion() {
        return "1.1.";
    }
}
