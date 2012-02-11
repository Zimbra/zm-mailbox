/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZDocument implements ZItem, ToZJSONObject {

	private boolean isWiki;
	private String name;
	private String id;
	private String uuid;
	private String folderId;
	private String version;
	private String editor;
	private String creator;
	private String restUrl;
	private long createdDate;
	private long modifiedDate;
	private long metadataChangeDate;
	private long size;
	private String contentType;
	private String tagIds;
	private String flags;

    public ZDocument(Element e) throws ServiceException {
    	isWiki = "w".equals(e.getName());
    	name = e.getAttribute(MailConstants.A_NAME);
    	id = e.getAttribute(MailConstants.A_ID);
    	uuid = e.getAttribute(MailConstants.A_UUID, null);
    	folderId = e.getAttribute(MailConstants.A_FOLDER);
    	version = e.getAttribute(MailConstants.A_VERSION);
    	editor = e.getAttribute(MailConstants.A_LAST_EDITED_BY);
    	creator = e.getAttribute(MailConstants.A_CREATOR);
    	restUrl = e.getAttribute(MailConstants.A_REST_URL, null);
    	createdDate = e.getAttributeLong(MailConstants.A_CREATED_DATE, 0) * 1000;
    	modifiedDate = e.getAttributeLong(MailConstants.A_DATE, 0);
    	metadataChangeDate = e.getAttributeLong(MailConstants.A_CHANGE_DATE, 0) * 1000;
        size = e.getAttributeLong(MailConstants.A_SIZE,0);
        if(isWiki){
            contentType = MimeConstants.CT_TEXT_HTML; //"text/html";
        }else{
            contentType = e.getAttribute(MailConstants.A_CONTENT_TYPE);
        }
        tagIds = e.getAttribute(MailConstants.A_TAGS, null);
        flags = e.getAttribute(MailConstants.A_FLAGS, null);
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("name", name);
        zjo.put("id", id);
        zjo.put("uuid", uuid);
        zjo.put("folderId", folderId);
        zjo.put("version", version);
        zjo.put("editor", editor);
        zjo.put("createor", creator);
        zjo.put("restUrl", restUrl);
        zjo.put("createdDate", createdDate);
        zjo.put("modifiedDate", modifiedDate);
        zjo.put("metaDataChangedDate", metadataChangeDate);
        zjo.put("size", size);
        zjo.put("contentType", contentType);
        zjo.put("tags", tagIds);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZDocument %s]", id);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getFolderId() {
        return folderId;
    }

    public String getVersion() {
        return version;
    }

    public String getEditor() {
        return editor;
    }

    public String getCreator() {
        return creator;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public long getMetaDataChangedDate() {
        return metadataChangeDate;
    }

    public boolean isWiki() {
        return isWiki;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getTagIds() {
        return tagIds;
    }

    public String getFlags() {
        return flags;
    }

    @Override
    public void modifyNotification(ZModifyEvent event) {
    }
}
