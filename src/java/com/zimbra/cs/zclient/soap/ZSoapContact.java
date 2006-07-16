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

package com.zimbra.cs.zclient.soap;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZContact;
import com.zimbra.soap.Element;

class ZSoapContact implements ZContact, ZSoapItem {

    private String mId;
    private String mFlags;
    private String mFolderId;
    private String mTagIds;
    private String mRevision;
    private long mMetaDataChangedDate;
    private Map<String, String> mAttrs;
    
    ZSoapContact(Element e) throws ServiceException {
        mId = e.getAttribute(MailService.A_ID);
        mFolderId = e.getAttribute(MailService.A_FOLDER);
        mFlags = e.getAttribute(MailService.A_FLAGS, null);
        mTagIds = e.getAttribute(MailService.A_TAGS, null);
        mRevision = e.getAttribute(MailService.A_REVISION, null);
        mMetaDataChangedDate = e.getAttributeLong(MailService.A_MODIFIED_DATE, 0) * 1000;
        mAttrs = new HashMap<String, String>();
        for (Element a : e.listElements(MailService.E_ATTRIBUTE)) {
            mAttrs.put(a.getAttribute(MailService.A_ATTRIBUTE_NAME), a.getText());
        }
    }

    public String getFolderId() {
        return mFolderId;
    }

    public String getId() {
        return mId;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("folder", mFolderId);
        sb.add("flags", mFlags);
        sb.add("tags", mTagIds);
        sb.addDate("metaDataChangedDate", mMetaDataChangedDate);
        sb.add("revision", mRevision);
        sb.beginStruct("attrs");
        for (Map.Entry<String, String> entry : mAttrs.entrySet()) {
            sb.add(entry.getKey(), entry.getValue());
        }
        sb.endStruct();
        sb.endStruct();
        return sb.toString();
    }

    public String getFlags() {
        return mFlags;
    }

    public Map<String, String> getAttrs() {
        return mAttrs;
    }

    public long getMetaDataChangedDate() {
        return mMetaDataChangedDate;
    }

    public String getRevision() {
        return mRevision;
    }

    public String getTagIds() {
        return mTagIds;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;        
    }

    public boolean hasTags() {
        return mTagIds != null && mTagIds.length() > 0;
    }

}
