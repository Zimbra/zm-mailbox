/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Mar 29, 2005
 */
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.ZimbraNamespace;

/**
 * @author dkarp
 */
public class SoapJSProtocol extends SoapProtocol {

    private static final String NS_STR = "urn:zimbraSoap";
    private static final Namespace NS = Namespace.get(NS_PREFIX, NS_STR);
    private static final QName CODE = QName.get("Code", NS);
    private static final QName REASON = QName.get("Reason", NS);
    private static final String TEXT = "Text";
    private static final QName DETAIL = QName.get("Detail", NS);
    private static final String VALUE = "Value";
    private static final QName SENDER_CODE = QName.get("Sender", NS);
    private static final QName RECEIVER_CODE = QName.get("Receiver", NS);


    SoapJSProtocol()  { super(); }

    public Element.ElementFactory getFactory()  { return Element.JSONElement.mFactory; }

    public Namespace getNamespace()  { return NS; }

    public SoapFaultException soapFault(Element fault) {
        if (!isFault(fault))
            return new SoapFaultException("not a soap fault ", fault);

        Element reason = fault.getOptionalElement(REASON);
        String reasonText = (reason == null ? null : reason.getAttribute(TEXT, null));
        reasonText = (reasonText != null ? reasonText.trim() : "unknown reason");

        Element detail = fault.getOptionalElement(DETAIL);

        Element code = fault.getOptionalElement(CODE);
        String whoseFault = (code == null ? null : code.getAttribute(VALUE, null));
        boolean isReceiversFault = RECEIVER_CODE.getQualifiedName().equals(whoseFault);

        return new SoapFaultException(reasonText, detail, isReceiversFault, fault);
    }

    public Element soapFault(ServiceException e) {
        String reason = e.getMessage();
        if (reason == null)
            reason = e.toString();

        QName code = e.isReceiversFault() ? RECEIVER_CODE : SENDER_CODE;

        Element eFault = mFactory.createElement(mFaultQName);
        // FIXME: should really be a qualified "attribute"
        eFault.addUniqueElement(CODE).addAttribute(VALUE, code.getQualifiedName());
        // FIXME: should really be a qualified "attribute"
        eFault.addUniqueElement(REASON).addAttribute(TEXT, reason);
        // FIXME: should really be a qualified "attribute"
        Element eError = eFault.addUniqueElement(DETAIL).addUniqueElement(ZimbraNamespace.E_ERROR);
        eError.addAttribute(ZimbraNamespace.E_CODE.getName(), e.getCode());
        eError.addAttribute(ZimbraNamespace.E_TRACE.getName(), e.getId());

        if (e.getArgs() != null) {
            for (ServiceException.Argument arg : e.getArgs()) {
                Element val = eError.addElement(ZimbraNamespace.E_ARGUMENT);
                val.addAttribute(ZimbraNamespace.A_ARG_NAME, arg.mName);
                val.addAttribute(ZimbraNamespace.A_ARG_TYPE, arg.mType.toString());
                val.setText(arg.mValue);
            }
        }
        return eFault;
    }

    public String getContentType() {
        return "text/javascript; charset=utf-8";
    }

    public boolean hasSOAPActionHeader() {
        return false;
    }

    public String getVersion() {
        return "0.1.";
    }
}
