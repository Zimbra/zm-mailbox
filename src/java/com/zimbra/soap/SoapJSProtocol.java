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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 29, 2005
 */
package com.zimbra.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;

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

    public Element.ElementFactory getFactory()  { return Element.JavaScriptElement.mFactory; }

//    public Namespace getNamespace()  { return NS; }
    public Namespace getNamespace()  { return Namespace.NO_NAMESPACE; }

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
        QName code = (e.isReceiversFault() ? RECEIVER_CODE : SENDER_CODE);

        Element eFault = mFactory.createElement(mFaultQName);
        // FIXME: should really be a qualified "attribute"
        eFault.addUniqueElement(CODE).addAttribute(VALUE, code.getQualifiedName());
        // FIXME: should really be a qualified "attribute"
        eFault.addUniqueElement(REASON).addAttribute(TEXT, reason);
        // FIXME: should really be a qualified "attribute"
        eFault.addUniqueElement(DETAIL).addUniqueElement(ZimbraNamespace.E_ERROR)
              .addAttribute(ZimbraNamespace.E_CODE.getName(), e.getCode());
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
