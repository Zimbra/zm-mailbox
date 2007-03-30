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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zclient.ZFolder.View;
import org.dom4j.DocumentException;

public class ZShare {

    /*
    <share xmlns="urn:zimbraShare" version="0.1" action="new" >
      <grantee id="4a4894bc-6d63-4589-afe8-b19352ffa779" email="user1@macpro.local" name="user1@macpro.local" />
      <grantor id="84ad414f-baef-43c1-938d-9f1ecf2a2489" email="user3@macpro.local" name="Demo User Three" />
      <link id="10" name="Calendar" view="appointment" perm="r" />
      <notes></notes>
    </share>
     */

    public static final String E_SHARE = "share";
    public static final String E_GRANTEE = "grantee";
    public static final String E_GRANTOR = "grantor";
    public static final String E_LINK = "link";
    public static final String E_NOTES = "notes";

    public static final String A_ID = "id";
    public static final String A_EMAIL = "email";
    public static final String A_NAME = "name";
    public static final String A_VIEW = "view";
    public static final String A_PERM = "perm";
    public static final String A_ACTION = "action";
    public static final String A_VERSION = "version";

    private ZGrantee mGrantee;
    private ZGrantor mGrantor;
    private ZLink mLink;
    private String mVersion;
    private ZShareAction mAction;

    public ZShare() {

    }

    public static ZShare parseXml(String xml) throws ServiceException {
        try {
            return new ZShare(Element.parseXML(xml));
        } catch (DocumentException e) {
            throw ZClientException.ZIMBRA_SHARE_PARSE_ERROR("can't parse share", e);
        }
    }

    public ZShare(Element e) throws ServiceException {
        Element granteeEl = e.getOptionalElement(E_GRANTEE);
        if (granteeEl != null)
            mGrantee = new ZGrantee(granteeEl);

        Element grantorEl = e.getOptionalElement(E_GRANTOR);
        if (grantorEl != null)
            mGrantor = new ZGrantor(grantorEl);

        Element linkEl = e.getOptionalElement(E_LINK);
        if (linkEl != null)
            mLink = new ZLink(linkEl);

        mVersion = e.getAttribute(A_VERSION);
        mAction = ZShareAction.fromString(e.getAttribute(A_ACTION));
    }

    public Element toElement(Element parent) {
        Element e = parent.addElement(E_SHARE);
        e.addAttribute(A_VERSION, mVersion);
        e.addAttribute(A_ACTION, mAction.toString());
        if (mGrantee != null) mGrantee.toElement(e);
        if (mGrantor != null) mGrantor.toElement(e);
        if (mLink != null) return mLink.toElement(e);
        return e;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add(A_VERSION, mVersion);
        sb.add(A_ACTION, mAction.toString());
        sb.addStruct(E_GRANTEE, mGrantee.toString());
        sb.addStruct(E_GRANTOR, mGrantor.toString());
        sb.addStruct(E_LINK, mLink.toString());
        sb.endStruct();
        return sb.toString();
    }

    public ZGrantee getGrantee() {
        return mGrantee;
    }

    public void setGrantee(ZGrantee grantee) {
        mGrantee = grantee;
    }

    public ZGrantor getGrantor() {
        return mGrantor;
    }

    public void setGrantor(ZGrantor grantor) {
        mGrantor = grantor;
    }

    public ZLink getLink() {
        return mLink;
    }

    public void setLink(ZLink link) {
        mLink = link;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public ZShareAction getAction() {
        return mAction;
    }

    public void setAction(ZShareAction action) {
        mAction = action;
    }

    public enum ZShareAction {
        NEW, EDIT, DELETE, ACCEPT, DECLINE;

        public static ZShareAction fromString(String s) throws ServiceException {
            try {
                return ZShareAction.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid share action "+s+", valid values: new, edit, delete, accept, decline", e);
            }
        }

        public String toString() {
            return name().toLowerCase();
        }
    }

    public static class ZGrantInfo {

        private String mId;
        private String mEmail;
        private String mName;

        public ZGrantInfo() {

        }

        public ZGrantInfo(Element e) throws ServiceException {
            mId = e.getAttribute(A_ID);
            mEmail = e.getAttribute(A_EMAIL);
            mName =  e.getAttribute(A_NAME);
        }

        protected Element toElement(Element parent, String name) {
            Element el = parent.addElement(name);
            el.addAttribute(A_ID, mId);
            el.addAttribute(A_EMAIL, mEmail);
            el.addAttribute(A_NAME, mName);
            return el;
        }

        public String getId() {
            return mId;
        }

        public void setId(String id) {
            mId = id;
        }

        public String getEmail() {
            return mEmail;
        }

        public void setEmail(String email) {
            mEmail = email;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add(A_ID, mId);
            sb.add(A_EMAIL, mEmail);
            sb.add(A_NAME, mName);
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }
    }

    public static class ZGrantor extends ZGrantInfo {
        public ZGrantor() {

        }

        public ZGrantor(Element e) throws ServiceException {
            super(e);
        }

        public Element toElement(Element parent) {
            return super.toElement(parent, E_GRANTOR);
        }
    }

    public static class ZGrantee extends ZGrantInfo {
        public ZGrantee() {

        }

        public ZGrantee(Element e) throws ServiceException {
            super(e);
        }

        public Element toElement(Element parent) {
            return super.toElement(parent, E_GRANTEE);
        }
    }


    public static class ZLink {

        private String mId;
        private ZFolder.View mView;
        private String mName;
        private String mPermission;

        public ZLink() {

        }

        public ZLink(Element e) throws ServiceException {
            mId = e.getAttribute(A_ID);
            mPermission = e.getAttribute(A_PERM);
            mName =  e.getAttribute(A_NAME);
            mView = ZFolder.View.fromString(e.getAttribute(A_VIEW));
        }

        protected Element toElement(Element parent) {
            Element el = parent.addElement(E_LINK);
            el.addAttribute(A_ID, mId);
            el.addAttribute(A_NAME, mName);
            el.addAttribute(A_VIEW, mView.name());
            el.addAttribute(A_PERM, mPermission);
            return el;
        }

        public View getView() {
            return mView;
        }

        public void setView(View view) {
            mView = view;
        }

        public String getId() {
            return mId;
        }

        public void setId(String id) {
            mId = id;
        }

        public String getPermission() {
            return mPermission;
        }

        public void setPermission(String perm) {
            mPermission = perm;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add(A_ID, mId);
            sb.add(A_PERM, mPermission);
            sb.add(A_VIEW, mView.name());
            sb.add(A_NAME, mName);
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }
    }
}
