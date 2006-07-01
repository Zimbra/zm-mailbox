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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap;

import com.zimbra.cs.service.ServiceException;

public class SoapFaultException extends ServiceException {

    /**
     * IO exception happened
     */
    public static final String IO_ERROR        = "soap.IO_ERROR";
    
    /**
     * generic client error
     */
    public static final String CLIENT_ERROR    = "soap.CLIENT_ERROR";    
    
    /**
     * used for default value and when we get a fault without a detail code
     */
    public static final String UNKNOWN = "soap.UNKNOWN";     

    public static SoapFaultException IO_ERROR(String msg, Throwable cause) {
        return new SoapFaultException(msg, IO_ERROR, SENDERS_FAULT, cause);
    }

    public static SoapFaultException CLIENT_ERROR(String msg, Throwable cause) {
        return new SoapFaultException(msg, CLIENT_ERROR, SENDERS_FAULT, cause);
    }

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




