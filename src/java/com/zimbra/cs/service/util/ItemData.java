/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.util;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Tag;

public class ItemData {
    public String sender, extra, flags, path, tags;
    public MailItem.UnderlyingData ud;
    private String tagsOldFmt = null;
    
    private static enum Keys {
        id, type, parent_id, folder_id, index_id, imap_id, date, size,
        volume_id, blob_digest, unread, flags, tags, subject, name,
        metadata, mod_metadata, change_date, mod_content,
        sender, ExtraStr, FlagStr, Path, TagStr, TagNames, Ver
    }

    public ItemData(MailItem mi) throws IOException {
        this(mi, null);
    }

    public ItemData(MailItem mi, String userData) throws IOException {
        try {
            sender = mi.getSender();
            extra = userData;
            flags = mi.getFlagString();
            path = mi.getPath();
            tagsOldFmt = mi.getTagString();
            tags = getTagString(mi);
            ud = mi.getUnderlyingData();
        } catch (Exception e) {
            throw new IOException("data error: " + e);
        }
    }
    
    public ItemData(final String encoded) throws IOException {
        try {
            JSONObject json = new JSONObject(encoded);
            int version = (byte)json.getInt(Keys.Ver.toString());
            
            if (version > Metadata.CURRENT_METADATA_VERSION)
                throw new IOException("unsupported data version");
            ud = new MailItem.UnderlyingData();
            ud.id = json.getInt(Keys.id.toString());
            ud.type = (byte)json.getInt(Keys.type.toString());
            ud.parentId = json.getInt(Keys.parent_id.toString());
            ud.folderId = json.getInt(Keys.folder_id.toString());
            /*
             * indexId and volumeId changed to optional strings from mandatory
             *  ints which breaks sync with 5.0 clients 
            
             * ud.indexId = json.optString(Keys.index_id.toString());
             * ud.locator = json.getString(Keys.volume_id.toString());
             */
            ud.indexId = null;
            ud.locator = null;
            ud.imapId = json.getInt(Keys.imap_id.toString());
            ud.date = json.getInt(Keys.date.toString());
            ud.size = json.getLong(Keys.size.toString());
            ud.setBlobDigest(json.optString(Keys.blob_digest.toString()));
            ud.unreadCount = json.getInt(Keys.unread.toString());
            ud.flags = json.getInt(Keys.flags.toString()) |
                Flag.BITMASK_UNCACHED;
            ud.tags = json.getLong(Keys.tags.toString());
            ud.subject = json.optString(Keys.subject.toString());
            ud.name = json.optString(Keys.name.toString());
            ud.metadata = json.optString(Keys.metadata.toString());
            ud.modMetadata = json.getInt(Keys.mod_metadata.toString());
            ud.dateChanged = json.getInt(Keys.change_date.toString());
            ud.modContent = json.getInt(Keys.mod_content.toString());
            sender = json.optString(Keys.sender.toString());
            extra = json.optString(Keys.ExtraStr.toString());
            flags = json.optString(Keys.FlagStr.toString());
            path = json.optString(Keys.Path.toString());
            getTagsFromJson(json);
        } catch (JSONException e) {
            throw new IOException("decode error: " + e);
        }
    }
  
    public ItemData(final byte[] encoded) throws IOException {
        this(new String(encoded, "UTF-8"));
    }
    
    public JSONObject toJSON() throws IOException {
        try {
            return new JSONObject().
                put(Keys.id.toString(), ud.id).
                put(Keys.type.toString(), ud.type).
                put(Keys.parent_id.toString(), ud.parentId).
                put(Keys.folder_id.toString(), ud.folderId).
                // put(Keys.index_id.toString(), ud.indexId).
                put(Keys.index_id.toString(), 0).
                put(Keys.imap_id.toString(), ud.imapId).
                put(Keys.date.toString(), ud.date).
                put(Keys.size.toString(), ud.size).
                // put(Keys.volume_id.toString(), ud.locator).
                put(Keys.volume_id.toString(), 0).
                putOpt(Keys.blob_digest.toString(), ud.getBlobDigest()).
                put(Keys.unread.toString(), ud.unreadCount).
                put(Keys.flags.toString(), ud.flags).
                put(Keys.tags.toString(), ud.tags).
                putOpt(Keys.subject.toString(), ud.subject).
                putOpt(Keys.name.toString(), ud.name).
                putOpt(Keys.metadata.toString(), ud.metadata).
                put(Keys.mod_metadata.toString(), ud.modMetadata).
                put(Keys.change_date.toString(), ud.dateChanged).
                put(Keys.mod_content.toString(), ud.modContent).
                putOpt(Keys.sender.toString(), sender).
                putOpt(Keys.ExtraStr.toString(), extra).
                putOpt(Keys.FlagStr.toString(), flags).
                put(Keys.Path.toString(), path).
                putOpt(Keys.TagStr.toString(), tagsOldFmt).
                putOpt(Keys.TagNames.toString(), tags).
                put(Keys.Ver.toString(), Metadata.CURRENT_METADATA_VERSION);
        } catch (Exception e) {
            throw new IOException("encode error: " + e);
        }
    }

    public String encode(int indent) throws IOException {
        try {
            return toJSON().toString(indent);
        } catch (Exception e) {
            throw new IOException("encode error: " + e);
        }
    }

    public byte[] encode() throws IOException {
        try {
            return toJSON().toString().getBytes("UTF-8");
        } catch (Exception e) {
            throw new IOException("encode error: " + e);
        }
    }

    private String getTagString(MailItem mi) throws IOException {
        String tags = "";

        try {
            for (Tag tag : mi.getTagList()) {
                if (tags.length() > 0)
                    tags += ':';
                tags += tag.getName();
            }
        } catch (Exception e) {
            throw new IOException("tag error: " + e);
        }
        return tags;
    }
    
    private boolean isOldTags() {
        return tags.length() > 0 && Character.isDigit(tags.charAt(0)) && tags.indexOf(":") == -1;
    }
    
    private void getTagsFromJson(JSONObject json) {
        tags = json.optString(Keys.TagNames.toString()); // 6.0.7+ format (TagNames="<tag-name>[:<tag-name>]")
        if (tags.length() == 0) {
            // either 6.0.6 format (TagStr=":<tag-name>[:<tag-name>]"), or pre-6.0.6 format (TagStr="<tag int>[,<tag int>]")
            tags = json.optString(Keys.TagStr.toString()); 
            if (isOldTags()) // is pre-6.0.6
                tagsOldFmt = tags;
        }
    }
    
    public boolean tagsEqual(MailItem mi) throws IOException {
        return isOldTags() ? tags.equals(mi.getTagString()) : tags.equals(getTagString(mi));
    }
}
