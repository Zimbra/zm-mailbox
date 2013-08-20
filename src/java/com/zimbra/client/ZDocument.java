/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ZDocument implements ZItem, ToZJSONObject {

    private final boolean isWiki;
    private final String name;
    private final String id;
    private final String uuid;
    private final String folderId;
    private final String version;
    private final String editor;
    private final String creator;
    private final String restUrl;
    private final long createdDate; /* Revision creation date - millis since 1970-01-01 00:00 UTC. */
    private final long modifiedDate; /* content last modified - millis since 1970-01-01 00:00 UTC. */
    private final long metadataChangeDate; /* metadata &/or content last modified - millis since 1970-01-01 00:00 UTC */
    private final long size;
    private String contentType;
    private final String tagIds;
    private final String flags;

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
        createdDate = e.getAttributeLong(MailConstants.A_CREATED_DATE, 0);
        modifiedDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        /* Note: attribute MailConstants.A_CHANGE_DATE is in seconds, not milliseconds */
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

    public String getNameURLEncoded() {
        try {
            return URLEncoder.encode(name, "utf-8").replace("+", "%20");
        }
        catch (UnsupportedEncodingException e) {
            return name;
        }
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
