/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

public class LmcFolderActionRequest extends LmcSoapRequest {

        private String mIDList;

        private String mOp;

        private String mTargetFolder;

        private String mName;

        private String mPerm;
        private String mGrantee;
        private String mD;

        /**
         * Set the list of Folder ID's to operate on
         * @param idList - a list of the folders to operate on
         */
        public void setFolderList(String idList) {
                mIDList = idList;
        }

        /**
         * Set the operation
         * @param op - the operation (delete, read, etc.)
         */
        public void setOp(String op) {
                mOp = op;
        }

        public void setName(String t) {
                mName = t;
        }

        public void setTargetFolder(String f) {
                mTargetFolder = f;
        }

        public void setGrant(String perm, String grantee, String d) {
            mPerm = perm;
            mGrantee = grantee;
            mD = d;
        }

        public String getFolderList() {
                return mIDList;
        }

        public String getOp() {
                return mOp;
        }

        public String getTargetFolder() {
                return mTargetFolder;
        }

        public String getName() {
                return mName;
        }

        protected Element getRequestXML() {
                Element request = DocumentHelper.createElement(MailConstants.FOLDER_ACTION_REQUEST);
                Element a = DomUtil.add(request, MailConstants.E_ACTION, "");
                if (mIDList != null)
                    DomUtil.addAttr(a, MailConstants.A_ID, mIDList);
                if (mOp != null) {
                    DomUtil.addAttr(a, MailConstants.A_OPERATION, mOp);
                    if (mOp.equals("grant") ||
                            mOp.equals("!grant")) {
                        Element grant = DomUtil.add(a, MailConstants.E_GRANT, "");
                        if (mPerm != null)
                            DomUtil.addAttr(grant, MailConstants.A_RIGHTS, mPerm);
                        if (mGrantee != null)
                            DomUtil.addAttr(grant, MailConstants.A_GRANT_TYPE, mGrantee);
                        if (mD != null)
                            DomUtil.addAttr(grant, MailConstants.A_DISPLAY, mD);
                    }
                }
                if (mName != null)
                    DomUtil.addAttr(a, MailConstants.A_NAME, mName);
                if (mTargetFolder != null)
                    DomUtil.addAttr(a, MailConstants.A_FOLDER, mTargetFolder);
                return request;
        }

        protected LmcSoapResponse parseResponseXML(Element responseXML)
                        throws ServiceException {
                LmcFolderActionResponse response = new LmcFolderActionResponse();
                Element a = DomUtil.get(responseXML, MailConstants.E_ACTION);
                response.setFolderList(DomUtil.getAttr(a, MailConstants.A_ID));
                response.setOp(DomUtil.getAttr(a, MailConstants.A_OPERATION));
                return response;
        }
}