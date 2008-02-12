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

	private static final long serialVersionUID = 2021293100288028461L;

	/** used for default value and when we get a fault without a detail code */
    public static final String UNKNOWN = "soap.UNKNOWN";

    private boolean mIsReceiversFault;
    private Element mDetail;
    private Element mFault;
    private boolean mIsLocal;
    
    // in case catcher of SoapFaultException wants to see the request/response
    private String mRequest;
    private String mResponse;

    public SoapFaultException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault);
    }

    /** Create a new SoapFaultException. */
    public SoapFaultException(String message, Element detail, boolean isReceiversFault) {
        super(message, getCode(detail), isReceiversFault);
        mIsReceiversFault = isReceiversFault;
        //subcode = subcode;
        mDetail = detail;
        mFault = null;
    }

    /** Create a new SoapFaultException. Used by SoapProtocol only. */
    SoapFaultException(String message, Element detail, boolean isReceiversFault, Element fault) {
        super(message, getCode(detail), isReceiversFault);
        mIsReceiversFault = isReceiversFault;
        mDetail = detail;
        mFault = fault;
        Element error = detail.getOptionalElement(ZimbraNamespace.E_ERROR);
        if (error != null) {
            String traceId = error.getAttribute(ZimbraNamespace.E_TRACE.getName(), null);
            if (traceId != null) setId(traceId);
        }
    }

    /** used by transports and stub mCode */
    public SoapFaultException(String message, Element fault) {
        super(message, UNKNOWN, false);
        mFault = fault;
    }

    /** used by transports and stub mCode */
    public SoapFaultException(String message, boolean isLocal, Element fault) {
        this(message, fault);
        mIsLocal = isLocal;
    }


    private static String getCode(Element detail) {
        Element error = detail.getOptionalElement(ZimbraNamespace.E_ERROR);
        if (error != null)
            return error.getAttribute(ZimbraNamespace.E_CODE.getName(), UNKNOWN);
        return UNKNOWN;
    }

    private static final String[] FAULT_CODE_PATH = new String[] {
        ZimbraNamespace.E_DETAIL.getName(), ZimbraNamespace.E_ERROR.getName(), ZimbraNamespace.E_CODE.getName()
    };

    /** Returns the error code. */
    @Override public String getCode() {
        return (mFault == null ? null : mFault.getPathAttribute(FAULT_CODE_PATH));
    }

    private static final String[] FAULT_ARGUMENT_PATH = new String[] {
        ZimbraNamespace.E_DETAIL.getName(), ZimbraNamespace.E_ERROR.getName(), ZimbraNamespace.E_ARGUMENT.getName()
    };

    /** Returns the value for the given argument, or <tt>null</tt> if the
     *  argument could not be found. */
    public String getArgumentValue(String argumentName) {
        if (mFault == null)
            return null;

        List<Element> arguments = mFault.getPathElementList(FAULT_ARGUMENT_PATH);
        if (arguments == null)
            return null;

        for (Element argument : arguments) {
            String name = argument.getAttribute(ZimbraNamespace.A_ARG_NAME, null);
            if (StringUtil.equal(name, argumentName))
                return argument.getText();
        }
        return null;
    }
    
    private static final String[] FAULT_REASON_PATH = new String[] {
        ZimbraNamespace.E_REASON.getName(), ZimbraNamespace.E_TEXT.getName()
    };

    /** Returns the reason for the fault. */
    public String getReason() {
        return (mFault == null ? null : mFault.getPathAttribute(FAULT_REASON_PATH));
    }

    public Element getDetail() {
        return mDetail;
    }

    /** can only be called if mDetail is null. */
    protected void initDetail(Element detail) throws IllegalStateException {
        if (mDetail != null)
            throw new IllegalStateException("mDetail is not null");

        mDetail = detail;
    }

    @Override public boolean isReceiversFault() {
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
     * Attache the request that caused this exception for downstream consumption
     * @param request
     */
    public void setFaultRequest(String request) {
    	mRequest = request;
    }
    
    /**
     * Attache the response that contains this exception for downstream consumption
     * @param response
     */
    public void setFaultResponse(String response) {
    	mResponse = response;
    }
    
    /**
     * Retrieves the request that caused this exception
     * @return the request, null if not available
     */
    public String getFaultRequest() {
    	return mRequest;
    }
    
    /**
     * Retrieves the response that contains this exception
     * @return the response, null if not available
     */
    public String getFaultResponse() {
    	return mResponse;
    }

    /**
     * dump out detailed debugging information about this mFault
     */
    public String dump() {
        StringBuffer sb = new StringBuffer();
        sb.append("class=").append(getClass().getName()).append("\n");
        sb.append("message=").append(getMessage()).append("\n");
        sb.append("mIsReceiversFault=").append(mIsReceiversFault).append("\n");
        sb.append("mIsLocal=").append(mIsLocal).append("\n");

        sb.append("mDetail=").append(mDetail).append("\n");
        sb.append("mFault=").append(mFault).append("\n");
        return sb.toString();
    }
}




