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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

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
@XmlType(propOrder = {"metadatas", "grants", "retentionPolicy", "subfolders"})
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

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-tag uuid
     * @zm-api-field-description Item's UUID - a globally unique identifier
     */
    @XmlAttribute(name=MailConstants.A_UUID /* uuid */, required=true)
    private String uuid;

    /**
     * @zm-api-field-tag folder-name
     * @zm-api-field-description Name of folder; max length 128; whitespace is trimmed by server;
     * Cannot contain ':', '"', '/', or any character below 0x20
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag parent-id
     * @zm-api-field-description ID of parent folder (absent for root folder)
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String parentId;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags - checked in UI (#), exclude free/(b)usy info, IMAP subscribed (*),
     * does not (i)nherit rights from parent, is a s(y)nc folder with external data source,
     * sync is turned on(~), folder does n(o)t allow inferiors / children
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Integer color;

    /**
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

    /**
     * @zm-api-field-tag unread-count
     * @zm-api-field-description Number of unread messages in folder
     */
    @XmlAttribute(name=MailConstants.A_UNREAD /* u */, required=false)
    private Integer unreadCount;

    /**
     * @zm-api-field-tag imap-unread
     * @zm-api-field-description Number of unread messages with this tag, <b>including</b> those with the
     * <b>IMAP \Deleted</b> flag set
     */
    @XmlAttribute(name=MailConstants.A_IMAP_UNREAD /* i4u */, required=false)
    private Integer imapUnreadCount;

    /**
     * @zm-api-field-tag default-type
     * @zm-api-field-description (optional) Default type for the folder; used by web client to decide which view to use;
     * <br />
     * possible values are the same as <b>&lt;SearchRequest></b>'s {types}: <b>conversation|message|contact|etc</b>
     */
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW /* view */, required=false)
    private View view = View.UNKNOWN;

    /**
     * @zm-api-field-tag revision
     * @zm-api-field-description Revision
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    /**
     * @zm-api-field-tag modified-sequence
     * @zm-api-field-description Modified sequence
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-tag change-date
     * @zm-api-field-description Modified date in seconds
     */
    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    /**
     * @zm-api-field-tag msg-count
     * @zm-api-field-description Number of non-subfolder items in folder
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=false)
    private Integer itemCount;

    /**
     * @zm-api-field-tag imap-count
     * @zm-api-field-description Number of non-subfolder items in folder, <b>including</b> those with the
     * <b>IMAP \Deleted</b> flag set
     */
    @XmlAttribute(name=MailConstants.A_IMAP_NUM /* i4n */, required=false)
    private Integer imapItemCount;

    /**
     * @zm-api-field-tag total-size
     * @zm-api-field-description Total size of all of non-subfolder items in folder
     */
    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long totalSize;

    /**
     * @zm-api-field-tag imap-modified-seq
     * @zm-api-field-description Imap modified sequence
     */
    @XmlAttribute(name=MailConstants.A_IMAP_MODSEQ /* i4ms */, required=false)
    private Integer imapModifiedSequence;

    /**
     * @zm-api-field-tag imap-uidnext
     * @zm-api-field-description IMAP UIDNEXT
     */
    @XmlAttribute(name=MailConstants.A_IMAP_UIDNEXT /* i4next */, required=false)
    private Integer imapUidNext;

    /**
     * @zm-api-field-tag remote-url
     * @zm-api-field-description URL (RSS, iCal, etc.) this folder syncs its contents to
     */
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=false)
    private String url;
    
    @XmlAttribute(name=MailConstants.A_ACTIVESYNC_DISABLED /* activesyncdisabled */, required=false)
    private ZmBoolean activeSyncDisabled;

    /**
     * @zm-api-field-tag effective-perms
     * @zm-api-field-description Foor remote folders, the access rights the authenticated user has on the folder -
     * will contain the calculated (c)reate folder permission if the user has both (i)nsert and (r)ead access on the
     * folder
     */
    @XmlAttribute(name=MailConstants.A_RIGHTS /* perm */, required=false)
    private String perm;

    /**
     * @zm-api-field-tag recursive
     * @zm-api-field-description Recursive
     */
    @XmlAttribute(name=MailConstants.A_RECURSIVE /* recursive */, required=false)
    private ZmBoolean recursive;

    /**
     * @zm-api-field-tag rest-url
     * @zm-api-field-description URL to the folder in the REST interface for rest-enabled apps (such as notebook)
     */
    @XmlAttribute(name=MailConstants.A_REST_URL /* rest */, required=false)
    private String restUrl;

    /**
     * @zm-api-field-tag metadata
     * @zm-api-field-description Custom metadata
     */
    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<MailCustomMetadata> metadatas = Lists.newArrayList();

    /**
     * @zm-api-field-description Grants
     */
    @XmlElementWrapper(name=MailConstants.E_ACL /* acl */, required=false)
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private List<Grant> grants = new ArrayList<Grant>();

    /**
     * @zm-api-field-description Subfolders
     */
    @XmlElements({
        @XmlElement(name="folder", type=Folder.class),
        @XmlElement(name="link", type=Mountpoint.class),
        @XmlElement(name="search", type=SearchFolder.class)
    })
    private List<Folder> subfolders = new ArrayList<Folder>();

    /**
     * @zm-api-field-description Retention policy
     */
    @XmlElement(name=MailConstants.E_RETENTION_POLICY /* retentionPolicy */, required=false)
    private RetentionPolicy retentionPolicy;

    public Folder() {
    }

    public String getId() { return id; }
    public String getUuid() { return uuid; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public String getFlags() { return flags; }
    public Integer getColor() { return color; }
    public String getRgb() { return rgb; }
    public Integer getUnreadCount() { return unreadCount; }
    public Integer getImapUnreadCount() { return imapUnreadCount; }
    public Integer getItemCount() { return itemCount; }
    public Long getChangeDate() { return changeDate; }
    public Integer getImapItemCount() { return imapItemCount; }
    public Long getTotalSize() { return totalSize; }

    /**
     * Returns the {@code View}, or {@link View#UNKNOWN} if not specified.
     */
    public View getView() {
        return (view == null ? View.UNKNOWN : view);
    }

    public String getUrl() { return url; }
    public Boolean isActiveSyncDisabled() { return ZmBoolean.toBool(activeSyncDisabled); }
    public String getPerm() { return perm; }
    public List<Folder> getSubfolders() {
        return Collections.unmodifiableList(subfolders);
    }

    public String getRestUrl() { return restUrl; }

    public List<MailCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }

    public List<Grant> getGrants() {
        return Collections.unmodifiableList(grants);
    }

    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }
    public Integer getImapUidNext() { return imapUidNext; }
    public Integer getImapModifiedSequence() { return imapModifiedSequence; }
    public RetentionPolicy getRetentionPolicy() { return retentionPolicy; }
    public Boolean getRecursive() { return ZmBoolean.toBool(recursive); }

    public void setId(String id) { this.id = id; }
    public void setId(int id) { this.id = Integer.toString(id); }
    public void setUuid(String id) { this.uuid = id; }
    public void setName(String name) { this.name = name; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setFlags(String flags) { this.flags = flags; }

    public void setColor(Integer color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
    public void setImapUnreadCount(Integer imapUnreadCount) { this.imapUnreadCount = imapUnreadCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setImapItemCount(Integer imapItemCount) { this.imapItemCount = imapItemCount; }
    public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    public void setView(View view) { this.view = view; }
    public void setUrl(String url) { this.url = url; }
    public void setDisableActiveSync(Boolean disableActiveSync) { this.recursive = ZmBoolean.fromBool(disableActiveSync); }
    public void setPerm(String perm) { this.perm = perm; }
    public void setRestUrl(String restUrl) { this.restUrl = restUrl; }

    public void setSubfolders(Collection<Folder> folders) {
        subfolders.clear();
        if (folders != null) {
            subfolders.addAll(folders);
        }
    }

    public void setMetadatas(Iterable <MailCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public Folder addMetadata(MailCustomMetadata metadata) {
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

    public void setRecursive(Boolean recursive) { this.recursive = ZmBoolean.fromBool(recursive); }
}
