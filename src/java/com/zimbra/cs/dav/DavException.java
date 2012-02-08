/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

@SuppressWarnings("serial")
public class DavException extends Exception {
	protected boolean mStatusIsSet;
	protected int mStatus;
	protected Document mErrMsg;
	
	public DavException(String msg, int status) {
		super(msg);
		mStatus = status;
		mStatusIsSet = true;
	}
	
	public DavException(String msg, Throwable cause) {
		super(msg, cause);
		mStatusIsSet = false;
	}
	
	public DavException(String msg, int status, Throwable cause) {
		super(msg, cause);
		mStatus = status;
		mStatusIsSet = true;
	}

	public boolean isStatusSet() {
		return mStatusIsSet;
	}
	
	public int getStatus() {
		return mStatus;
	}
	
	public boolean hasErrorMessage() {
		return (mErrMsg != null);
	}
	
	public Element getErrorMessage() {
		if (mErrMsg == null)
			return null;
		return mErrMsg.getRootElement();
	}
	public void writeErrorMsg(OutputStream out) throws IOException {
		DomUtil.writeDocumentToStream(mErrMsg, out);
	}
	
	protected static class DavExceptionWithErrorMessage extends DavException {
		protected DavExceptionWithErrorMessage(String msg, int status) {
			super(msg, status);
			mErrMsg = org.dom4j.DocumentHelper.createDocument();
			mErrMsg.addElement(DavElements.E_ERROR);
		}
		protected void setError(QName error) {
			mErrMsg.getRootElement().addElement(error);
		}
		protected void setError(Element error) {
		    mErrMsg.getRootElement().add(error);
		}
	}
	public static class CannotModifyProtectedProperty extends DavExceptionWithErrorMessage {
		public CannotModifyProtectedProperty(QName prop) {
			super("property "+prop.getName()+" is protected", HttpServletResponse.SC_FORBIDDEN);
			setError(DavElements.E_CANNOT_MODIFY_PROTECTED_PROPERTY);
		}
	}
	public static class PropFindInfiniteDepthForbidden extends DavExceptionWithErrorMessage {
	    public PropFindInfiniteDepthForbidden() {
	        super("PROPFIND with infinite depth forbidden", HttpServletResponse.SC_FORBIDDEN);
	        setError(DavElements.E_PROPFIND_FINITE_DEPTH);
	    }
	}
	
    public static class UnsupportedReport extends DavExceptionWithErrorMessage {
        public UnsupportedReport(QName report) {
            super(report + " not implemented in REPORT", HttpServletResponse.SC_FORBIDDEN);
            Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_REPORT);
            e.addElement(DavElements.E_REPORT).addElement(report);
            setError(e);
        }
    }
}
