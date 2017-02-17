/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext.Depth;

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
        if (ZimbraLog.dav.isDebugEnabled()) {
            ZimbraLog.dav.debug("ERROR RESPONSE\n%s", mErrMsg.asXML());
        }
        DomUtil.writeDocumentToStream(mErrMsg, out);
    }

    protected static class DavExceptionWithErrorMessage extends DavException {
        protected DavExceptionWithErrorMessage(String msg, int status, Throwable cause) {
            super(msg, status, cause);
            mErrMsg = org.dom4j.DocumentHelper.createDocument();
            mErrMsg.addElement(DavElements.E_ERROR);
        }

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

    public static class InvalidData extends DavExceptionWithErrorMessage {
        public InvalidData(QName prop, String msg, Throwable cause) {
            super(msg, HttpServletResponse.SC_FORBIDDEN, cause);
            setError(prop);
        }
        public InvalidData(QName prop, String msg) {
            super(msg, HttpServletResponse.SC_FORBIDDEN);
            setError(prop);
        }
    }

    public static class UidConflict extends DavExceptionWithErrorMessage {
        public UidConflict(String msg, String href) {
            super(msg, HttpServletResponse.SC_PRECONDITION_FAILED);
            Element errElem = org.dom4j.DocumentHelper.createElement(DavElements.E_NO_UID_CONFLICT);
            Element hrefE= errElem.addElement(DavElements.E_HREF);
            hrefE.setText(href);
            setError(errElem);
        }
    }

    public static class CardDavUidConflict extends DavExceptionWithErrorMessage {
        public CardDavUidConflict(String msg, String href) {
            super(msg, HttpServletResponse.SC_PRECONDITION_FAILED);
            Element errElem = org.dom4j.DocumentHelper.createElement(DavElements.CardDav.E_NO_UID_CONFLICT);
            Element hrefE= errElem.addElement(DavElements.E_HREF);
            hrefE.setText(href);
            setError(errElem);
        }
    }

    /**
     * Associated with http://tools.ietf.org/html/rfc6638 CALDAV:same-organizer-in-all-components Precondition
     */
    public static class NeedSameOrganizerInAllComponents extends DavExceptionWithErrorMessage {
        public NeedSameOrganizerInAllComponents(String msg) {
            super(msg, HttpServletResponse.SC_FORBIDDEN);
            Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_SAME_ORGANIZER_IN_ALL_COMPONENTS);
            setError(e);
        }
    }

    public static class REPORTwithDisallowedDepthException extends DavException {
        public REPORTwithDisallowedDepthException(String reportName, Depth depth) {
            super(String.format("%s REPORT with %s depth is not allowed (only 0 is allowed)",
                    reportName, depth.toString()), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

}
