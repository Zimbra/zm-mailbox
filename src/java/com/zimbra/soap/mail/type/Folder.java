/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.CustomMetadata;
/*
<folder id="{folder-id}" name="{folder-name}" l="{parent-id}" [f="{flags}"] [color="{color}"]
               u="{unread}" [i4u="{imap-unread}"] n="{msg-count}" [i4n="{imap-count}"] s="{total-size}" [view="{default-type}"]
               [url="{remote-url}"] [perm="{effective-perms}"] [rest="{rest-url}"]>
  [<acl> <grant .../> </acl>]
</folder>
*/

// Root element name needed to differentiate between types of folder
// MailConstants.E_FOLDER == "folder"
@XmlRootElement(name=MailConstants.E_FOLDER)
@XmlType(propOrder = {"metadatas", "grants", "subfolders"})
public class Folder {

    @XmlEnum
    public enum View {
        @XmlEnumValue("") UNKNOWN (""),
        @XmlEnumValue("search folder") SEARCH_FOLDER ("search folder"),
        @XmlEnumValue("tag") TAG ("tag"),
        @XmlEnumValue("conversation") CONVERSATION ("conversation"),
        @XmlEnumValue("message") MESSAGE ("message"),
        @XmlEnumValue("contact") CONTACT ("contact"),
        @XmlEnumValue("document") DOCUMENT ("document"),
        @XmlEnumValue("appointment") APPOINTMENT ("appointment"),
        @XmlEnumValue("virtual conversation") VIRTUAL_CONVERSATION ("virtual conversation"),
        @XmlEnumValue("remote folder") REMOTE_FOLDER ("remote folder"),
        @XmlEnumValue("wiki") WIKI ("wiki"),
        @XmlEnumValue("task") TASK ("task"),
        @XmlEnumValue("chat") CHAT ("chat");

        private static Map<String, View> nameToView = new HashMap<String, View>();

        static {
            for (View v : View.values()) {
                nameToView.put(v.toString(), v);
            }
        }

        private String name;

        private View(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static View fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToView.get(name);
        }
    };

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private String id;

    @XmlAttribute(name=MailConstants.A_NAME, required=false)
    private String name;

    // MailConstants.A_FOLDER == "l"
    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String parentId;

    // MailConstants.A_FLAGS == "f"
    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_COLOR, required=false)
    private Integer color;

    @XmlAttribute(name=MailConstants.A_RGB, required=false)
    private String rgb;

    // MailConstants.A_UNREAD == "u"
    @XmlAttribute(name=MailConstants.A_UNREAD, required=false)
    private Integer unreadCount;

    // MailConstants.A_IMAP_UNREAD == "i4u"
    @XmlAttribute(name=MailConstants.A_IMAP_UNREAD, required=false)
    private Integer imapUnreadCount;

    // MailConstants.A_DEFAULT_VIEW == "view"
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW, required=false)
    private View view = View.UNKNOWN;

    // MailConstants.A_REVISION == "rev"
    @XmlAttribute(name=MailConstants.A_REVISION, required=false)
    private Integer revision;

    // MailConstants.A_MODIFIED_SEQUENCE == "ms"
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE, required=false)
    private Integer modifiedSequence;

    // MailConstants.A_CHANGE_DATE == "md"
    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=false)
    private Long changeDate;

    // MailConstants.A_NUM == "n"
    @XmlAttribute(name=MailConstants.A_NUM, required=false)
    private Integer itemCount;

    // MailConstants.A_IMAP_NUM == "i4n"
    @XmlAttribute(name=MailConstants.A_IMAP_NUM, required=false)
    private Integer imapItemCount;

    // MailConstants.A_SIZE == "s"
    @XmlAttribute(name=MailConstants.A_SIZE, required=false)
    private Long totalSize;

    // MailConstants.A_IMAP_MODSEQ == "i4ms"
    @XmlAttribute(name=MailConstants.A_IMAP_MODSEQ, required=false)
    private Integer imapModifiedSequence;

    // MailConstants.A_IMAP_UIDNEXT == "i4next"
    @XmlAttribute(name=MailConstants.A_IMAP_UIDNEXT, required=false)
    private Integer imapUidNext;

    // MailConstants.A_URL == "url"
    @XmlAttribute(name=MailConstants.A_URL, required=false)
    private String url;

    // MailConstants.A_RIGHTS == "perm"
    @XmlAttribute(name=MailConstants.A_RIGHTS, required=false)
    private String perm;

    // MailConstants.A_REST_URL == "rest"
    @XmlAttribute(name=MailConstants.A_REST_URL, required=false)
    private String restUrl;

    @XmlElement(name=MailConstants.E_METADATA, required=false)
    private List<CustomMetadata> metadatas = Lists.newArrayList();

    // MailConstants.E_ACL == "acl"
    @XmlElementWrapper(name=MailConstants.E_ACL, required=false)
    // MailConstants.E_GRANT == "grant"
    @XmlElement(name=MailConstants.E_GRANT, required=false)
    private List<Grant> grants = new ArrayList<Grant>();

    @XmlElements({
        @XmlElement(name="folder", type=Folder.class),
        @XmlElement(name="link", type=Mountpoint.class),
        @XmlElement(name="search", type=SearchFolder.class)
    })
    private List<Folder> subfolders = new ArrayList<Folder>();

    public Folder() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public String getFlags() {
        return flags;
    }

    public Integer getColor() {
        return color;
    }

    public String getRgb() { return rgb; }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public Integer getImapUnreadCount() {
        return imapUnreadCount;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public Integer getImapItemCount() {
        return imapItemCount;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    /**
     * Returns the {@code View}, or {@link View#UNKNOWN} if not specified.
     */
    public View getView() {
        return (view == null ? View.UNKNOWN : view);
    }

    public String getUrl() {
        return url;
    }

    public String getPerm() {
        return perm;
    }

    public List<Folder> getSubfolders() {
        return Collections.unmodifiableList(subfolders);
    }

    public String getRestUrl() {
        return restUrl;
    }

    public List<CustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }

    public List<Grant> getGrants() {
        return Collections.unmodifiableList(grants);
    }

    public Integer getModifiedSequence() {
        return modifiedSequence;
    }

    public Integer getRevision() {
        return revision;
    }

    public Integer getImapUidNext() {
        return imapUidNext;
    }

    public Integer getImapModifiedSequence() {
        return imapModifiedSequence;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        this.id = Integer.toString(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public void setRgb(String rgb) { this.rgb = rgb; }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setImapUnreadCount(Integer imapUnreadCount) {
        this.imapUnreadCount = imapUnreadCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public void setImapItemCount(Integer imapItemCount) {
        this.imapItemCount = imapItemCount;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPerm(String perm) {
        this.perm = perm;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    public void setSubfolders(Collection<Folder> folders) {
        subfolders.clear();
        if (folders != null) {
            subfolders.addAll(folders);
        }
    }

    public void setMetadatas(Iterable <CustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public Folder addMetadata(CustomMetadata metadata) {
        this.metadatas.add(metadata);
        return this;
    }

    public void setGrants(Collection<Grant> grants) {
        this.grants.clear();
        if (grants != null) {
            this.grants.addAll(grants);
        }
    }

    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public void setImapUidNext(Integer imapUidNext) {
        this.imapUidNext = imapUidNext;
    }

    public void setImapModifiedSequence(Integer imapModifiedSequence) {
        this.imapModifiedSequence = imapModifiedSequence;
    }
}
