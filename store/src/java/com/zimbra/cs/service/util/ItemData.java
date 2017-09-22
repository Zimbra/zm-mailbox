/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.util;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.util.TagUtil;

public class ItemData {
    public String sender, extra, flags, path, tags;
    public MailItem.UnderlyingData ud;
    private String tagsOldFmt = null;

    private static enum Keys {
        id, uuid, type, parent_id, folder_id, index_id, imap_id, date, size,
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
            tagsOldFmt = TagUtil.getTagIdString(mi);
            tags = getTagString(mi.getTags());
            ud = mi.getUnderlyingData();
        } catch (Exception e) {
            throw new IOException("data error: " + e);
        }
    }

    public ItemData(final String encoded) throws IOException {
        try {
            JSONObject json = new JSONObject(encoded);
            int version = (byte)json.getInt(Keys.Ver.toString());

            if (version > Metadata.LEGACY_METADATA_VERSION) {
                throw new IOException("unsupported data version");
            }
            ud = new MailItem.UnderlyingData();
            ud.id = json.getInt(Keys.id.toString());
            String uuid = json.optString(Keys.uuid.toString());
            if (uuid != null && uuid.length() > 0) {  // because optString returns an empty string rather than null
                ud.uuid = uuid;
            }
            ud.type = (byte) json.getInt(Keys.type.toString());
            ud.parentId = json.getInt(Keys.parent_id.toString());
            ud.folderId = json.getInt(Keys.folder_id.toString());
            // indexId and volumeId changed to optional strings from mandatory ints which breaks sync with 5.0 clients
            ud.indexId = MailItem.IndexStatus.NO.id();
            ud.locator = null;
            ud.imapId = json.getInt(Keys.imap_id.toString());
            ud.date = json.getInt(Keys.date.toString());
            ud.size = json.getLong(Keys.size.toString());
            ud.setBlobDigest(json.optString(Keys.blob_digest.toString()));
            ud.unreadCount = json.getInt(Keys.unread.toString());
            ud.setFlags(json.getInt(Keys.flags.toString()) | Flag.BITMASK_UNCACHED);
//            ud.tags = json.getLong(Keys.tags.toString());
            ud.setSubject(json.optString(Keys.subject.toString()));
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
                put(Keys.uuid.toString(), ud.uuid).
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
                put(Keys.flags.toString(), ud.getFlags()).
//                put(Keys.tags.toString(), ud.getTags()).
                putOpt(Keys.subject.toString(), ud.getSubject()).
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
                put(Keys.Ver.toString(), Metadata.LEGACY_METADATA_VERSION);
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

    @VisibleForTesting
    public static String getTagString(String[] tags) {
        if (tags == null || tags.length == 0) {
            return "";
        }

        // we use colon-delimited strings for legacy reasons
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) {
                serialized.append(':');
            }
            serialized.append(tags[i].replace("\\", "\\\\").replace(":", "\\:"));
        }
        return serialized.toString();
    }

    public static String[] getTagNames(String serialized) {
        // we use colon-delimited strings for legacy reasons
        if (serialized.indexOf(':') == -1) {
            return new String[] { serialized };
        }

        StringBuilder tag = new StringBuilder();
        List<String> tags = Lists.newArrayList();
        boolean escaped = false;
        for (int i = 0, len = serialized.length(); i <= len; i++) {
            char c = i == len ? ':' : serialized.charAt(i);
            if (escaped) {
                tag.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == ':') {
                if (tag.length() > 0) {
                    tags.add(tag.toString());
                    tag.setLength(0);
                }
            } else {
                tag.append(c);
            }
        }
        return tags.toArray(new String[tags.size()]);
    }

    private boolean isOldTags() {
        return tags.length() > 0 && Character.isDigit(tags.charAt(0)) && tags.indexOf(":") == -1;
    }

    private void getTagsFromJson(JSONObject json) {
        tags = json.optString(Keys.TagNames.toString()); // 6.0.7+ format (TagNames="<tag-name>[:<tag-name>]")
        if (tags.length() == 0) {
            // either 6.0.6 format (TagStr=":<tag-name>[:<tag-name>]"), or pre-6.0.6 format (TagStr="<tag int>[,<tag int>]")
            tags = json.optString(Keys.TagStr.toString());
            if (isOldTags()) { // is pre-6.0.6
                tagsOldFmt = tags;
            }
        }
    }

    public boolean tagsEqual(MailItem mi) throws ServiceException {
        // FIXME: may not work with misordered tags
        return isOldTags() ? tags.equals(TagUtil.getTagIdString(mi)) : tags.equals(getTagString(mi.getTags()));
    }
}
