/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/*
<folder id="{folder-id}" name="{folder-name}" l="{parent-id}" [f="{flags}"] [color="{color}"]
               u="{unread}" [i4u="{imap-unread}"] n="{msg-count}" [i4n="{imap-count}"] s="{total-size}" [view="{default-type}"]
               [url="{remote-url}"] [perm="{effective-perms}"] [rest="{rest-url}"]>
  [<acl> <grant .../> </acl>]
</folder>
*/
@XmlRootElement(name="folder") // Root element is used for handling subclasses.
@XmlType(propOrder = {})
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
    
    @XmlAttribute private String id;
    @XmlAttribute private String name;
    @XmlAttribute(name="l") private String parentId;
    @XmlAttribute(name="f") private String flags;
    @XmlAttribute private Integer color;
    @XmlAttribute(name="u") private Integer unreadCount;
    @XmlAttribute(name="i4u") private Integer imapUnreadCount;
    @XmlAttribute(name="n") private Integer itemCount;
    @XmlAttribute(name="i4n") private Integer imapItemCount;
    @XmlAttribute(name="s") private Long totalSize;
    @XmlAttribute private View view = View.UNKNOWN;
    @XmlAttribute private String url;
    @XmlAttribute private String perm;
    @XmlAttribute(name="rest") private String restUrl;
    @XmlAttribute(name="ms") private Integer modifiedSequence;
    @XmlAttribute(name="ref") private Integer revision;
    @XmlAttribute(name="i4next") private Integer imapUidNext;
    @XmlAttribute(name="i4ms") private Integer imapModifiedSequence;
    
    @XmlElementRef private List<Folder> subfolders = new ArrayList<Folder>();
    @XmlElementWrapper(name="acl")
    @XmlElement(name="grant")
    private List<Grant> grants = new ArrayList<Grant>();
    
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
    
    public List<Grant> getGrants() { return Collections.unmodifiableList(grants); }
    
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
