/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 29, 2005
 */
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.ExceptionToString;

/**
 * @author dkarp
 */
public class SoapJSProtocol extends SoapProtocol {

    private static final String NS_STR = "urn:zimbraSoap";
    private static final Namespace NS = Namespace.get(NS_PREFIX, NS_STR);
    private static final QName CODE = QName.get("Code", NS);
    private static final QName REASON = QName.get("Reason", NS);
    private static final String TEXT = "Text";
    public static final QName DETAIL = QName.get("Detail", NS);
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
        if (LC.soap_fault_include_stack_trace.booleanValue())
            eError.addAttribute(ZimbraNamespace.E_TRACE.getName(), ExceptionToString.ToString(e));
        else
            eError.addAttribute(ZimbraNamespace.E_TRACE.getName(), e.getThreadName());

        for (ServiceException.Argument arg : e.getArgs()) {
            if (arg.externalVisible()) {
                Element val = eError.addElement(ZimbraNamespace.E_ARGUMENT);
                val.addAttribute(ZimbraNamespace.A_ARG_NAME, arg.name);
                val.addAttribute(ZimbraNamespace.A_ARG_TYPE, arg.type.toString());
                val.setText(arg.value);
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
