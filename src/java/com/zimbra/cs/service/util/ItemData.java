/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.service.util;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;

public class ItemData {
    public String flags, path, tags;
    public MailItem.UnderlyingData ud;
    private static enum Keys {
        Id, Type, PId, FId, Idx, Imap, Date, Size, Vol, Dgst, UC, Flags,
        Tags, From, Subj, Name, Meta, MNum, CDate, CNum,
        FlagStr, Path, TagStr, Ver
    }
    
    public ItemData(MailItem mi) throws IOException {
        try {
            flags = mi.getFlagString();
            path = mi.getPath();
            tags = mi.getTagString();
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
            ud.id = json.getInt(Keys.Id.toString());
            ud.type = (byte)json.getInt(Keys.Type.toString());
            ud.parentId = json.getInt(Keys.PId.toString());
            ud.folderId = json.getInt(Keys.FId.toString());
            ud.indexId = json.getInt(Keys.Idx.toString());
            ud.imapId = json.getInt(Keys.Imap.toString());
            ud.date = json.getInt(Keys.Date.toString());
            ud.size = json.getLong(Keys.Size.toString());
            ud.volumeId = (short)json.getInt(Keys.Vol.toString());
            ud.setBlobDigest(json.optString(Keys.Dgst.toString()));
            ud.unreadCount = json.getInt(Keys.UC.toString());
            ud.flags = json.getInt(Keys.Flags.toString()) |
                Flag.BITMASK_UNCACHED;
            ud.tags = json.getLong(Keys.Tags.toString());
            ud.sender = json.optString(Keys.From.toString());
            ud.subject = json.optString(Keys.Subj.toString());
            ud.name = json.optString(Keys.Name.toString());
            ud.metadata = json.optString(Keys.Meta.toString());
            ud.modMetadata = json.getInt(Keys.MNum.toString());
            ud.dateChanged = json.getInt(Keys.CDate.toString());
            ud.modContent = json.getInt(Keys.CNum.toString());
            flags = json.optString(Keys.FlagStr.toString());
            path = json.optString(Keys.Path.toString());
            tags = json.optString(Keys.TagStr.toString());
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
                put(Keys.Id.toString(), ud.id).
                put(Keys.Type.toString(), ud.type).
                put(Keys.PId.toString(), ud.parentId).
                put(Keys.FId.toString(), ud.folderId).
                put(Keys.Idx.toString(), ud.indexId).
                put(Keys.Imap.toString(), ud.imapId).
                put(Keys.Date.toString(), ud.date).
                put(Keys.Size.toString(), ud.size).
                put(Keys.Vol.toString(), ud.volumeId).
                putOpt(Keys.Dgst.toString(), ud.getBlobDigest()).
                put(Keys.UC.toString(), ud.unreadCount).
                put(Keys.Flags.toString(), ud.flags).
                put(Keys.Tags.toString(), ud.tags).
                putOpt(Keys.From.toString(), ud.sender).
                putOpt(Keys.Subj.toString(), ud.subject).
                putOpt(Keys.Name.toString(), ud.name).
                putOpt(Keys.Meta.toString(), ud.metadata).
                put(Keys.MNum.toString(), ud.modMetadata).
                put(Keys.CDate.toString(), ud.dateChanged).
                put(Keys.CNum.toString(), ud.modContent).
                putOpt(Keys.FlagStr.toString(), flags).
                put(Keys.Path.toString(), path).
                putOpt(Keys.TagStr.toString(), tags).
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
}
