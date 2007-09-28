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

package com.zimbra.common.soap;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;

public class SoapFaultException extends ServiceException {

    /**
     * used for default value and when we get a fault without a detail code
     */
    public static final String UNKNOWN = "soap.UNKNOWN";     

    private boolean mIsReceiversFault;
//    private QName mCode;
//    private QName subcode;
    private Element mDetail;
    private Element mFault;
    private boolean mIsLocal;

    
    public SoapFaultException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault);
    }


    /**
     * Create a new SoapFaultException.
     */
    public SoapFaultException(String message, Element detail, boolean isReceiversFault) {
        super(message, getCode(detail), isReceiversFault);
        mIsReceiversFault = isReceiversFault;
        //subcode = subcode;
        mDetail = detail;
        mFault = null;
    }

    /**
     * Create a new SoapFaultException. Used by SoapProtocol only.
     *
     */
    SoapFaultException(String message, Element detail, boolean isReceiversFault, Element fault) {
        super(message, getCode(detail), isReceiversFault);
        mIsReceiversFault = isReceiversFault;
        mDetail = detail;
        mFault = fault;
    }

    /**
     * used by transports and stub mCode
     */
    public SoapFaultException(String message, Element fault) {
        super(message, UNKNOWN, false);
        mFault = fault;
    }

    /**
     * used by transports and stub mCode
     */
    public SoapFaultException(String message, boolean isLocal, Element fault) {
        this(message, fault);
        mIsLocal = isLocal;
    }

    private static String getCode(Element detail) {
        Element error = detail.getOptionalElement(ZimbraNamespace.E_ERROR);
        if (error != null) {
            Element code = error.getOptionalElement(ZimbraNamespace.E_CODE);
            if (code != null)
                return code.getText();
        }
        return UNKNOWN;
    }
    
    /**
     * Returns the error code.
     */
    public String getCode() {
        if (mFault == null) {
            return null;
        }
        String[] path = new String[] {
            ZimbraNamespace.E_DETAIL.getName(),
            ZimbraNamespace.E_ERROR.getName(),
            ZimbraNamespace.E_CODE.getName()
        };
        Element code = mFault.getPathElement(path);
        if (code == null) {
            return null;
        }
        return code.getText();
    }
    
    /**
     * Returns the value for the given argument, or <tt>null</tt> if the
     * argument could not be found.
     */
    public String getArgumentValue(String argumentName) {
        if (mFault == null) {
            return null;
        }
        String[] path = new String[] {
            ZimbraNamespace.E_DETAIL.getName(),
            ZimbraNamespace.E_ERROR.getName(),
            ZimbraNamespace.E_ARGUMENT.getName()
        };
        List<Element> arguments = mFault.getPathElementList(path);
        if (arguments == null) {
            return null;
        }
        for (Element argument : arguments) {
            String name = argument.getAttribute(ZimbraNamespace.A_NAME, null);
            if (StringUtil.equal(name, argumentName)) {
                return argument.getText();
            }
        }
        return null;
    }
    
    /**
     * Returns the reason for the fault. 
     */
    public String getReason() {
        if (mFault == null) {
            return null;
        }
        String[] path = new String[] {
            ZimbraNamespace.E_REASON.getName(),
            ZimbraNamespace.E_TEXT.getName() };
        Element text = mFault.getPathElement(path);
        if (text == null) {
            return null;
        }
        return text.getText();
    }
    
    /*
    public QName getSubcode() {
        return subcode;
    }
    */

    public Element getDetail() {
        return mDetail;
    }

    /**
     * can only be called if mDetail is null.
     */
    protected void initDetail(Element detail) throws IllegalStateException {
        if (mDetail != null)
            throw new IllegalStateException("mDetail is not null");

        mDetail = detail;
    }

    public boolean isReceiversFault() {
        return mIsReceiversFault;
    }

    /**
     * Returns the raw soap mFault, if available.
     */
    public Element getFault() {
        return mFault;
    }

    /**
     * Returns whether the SOAP fault was generated locally.
     * <code>false</code> means that it came from a remote source.
     */
    public boolean isSourceLocal() {
        return mIsLocal;
    }

    /**
     * dump out detailed debugging information about this mFault
     */
    public String dump() {
        StringBuffer sb = new StringBuffer();
        sb.append("class=").append(getClass().getName()).append("\n");
        sb.append("message=").append(getMessage()).append("\n");
//        sb.append("mCode=").append(mCode).append("\n");
//        sb.append("subcode=").append(subcode).append("\n");
        sb.append("mIsReceiversFault=").append(mIsReceiversFault).append("\n");
        sb.append("mIsLocal=").append(mIsLocal).append("\n");

        sb.append("mDetail=").append(mDetail).append("\n");
        sb.append("mFault=").append(mFault).append("\n");
        return sb.toString();
    }
}




