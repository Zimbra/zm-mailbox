/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class WaitSetSessionInfo {

    private static Joiner COMMA_JOINER = Joiner.on(",");
    private static Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    /**
     * @zm-api-field-tag bitmask
     * @zm-api-field-description Interest bitmask
     */
    @XmlAttribute(name="interestMask", required=true)
    private final String interestMask;

    /**
     * @zm-api-field-tag mbox-change-id
     * @zm-api-field-description Mailbox change ID
     */
    @XmlAttribute(name="highestChangeId", required=true)
    private final int highestChangeId;

    /**
     * @zm-api-field-tag last-access-time
     * @zm-api-field-description Last access time
     */
    @XmlAttribute(name="lastAccessTime", required=true)
    private final long lastAccessTime;

    /**
     * @zm-api-field-tag creation-time
     * @zm-api-field-description Creation time
     */
    @XmlAttribute(name="creationTime", required=true)
    private final long creationTime;

    /**
     * @zm-api-field-tag session-id
     * @zm-api-field-description Session ID
     */
    @XmlAttribute(name="sessionId", required=true)
    private final String sessionId;

    /**
     * @zm-api-field-tag sync-token
     * @zm-api-field-description Sync Token
     */
    @XmlAttribute(name=MailConstants.A_TOKEN, required=false)
    private String token;

    private final Set<Integer> folderInterests = Sets.newHashSet();
    private final Set<Integer> changedFolders = Sets.newHashSet();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WaitSetSessionInfo() {
        this((String) null, -1, -1L, -1L, (String) null);
    }

    public WaitSetSessionInfo(String interestMask, int highestChangeId,
                long lastAccessTime, long creationTime, String sessionId) {
        this.interestMask = interestMask;
        this.highestChangeId = highestChangeId;
        this.lastAccessTime = lastAccessTime;
        this.creationTime = creationTime;
        this.sessionId = sessionId;
    }

    public void setToken(String token) { this.token = token; }
    public String getInterestMask() { return interestMask; }
    public int getHighestChangeId() { return highestChangeId; }
    public long getLastAccessTime() { return lastAccessTime; }
    public long getCreationTime() { return creationTime; }
    public String getSessionId() { return sessionId; }
    public String getToken() { return token; }

    /**
     * @zm-api-field-tag waitset-folder-interests
     * @zm-api-field-description Comma separated list of IDs for folders.
     */
    @XmlAttribute(name=MailConstants.A_FOLDER_INTERESTS /* folderInterests */, required=false)
    public String getFolderInterests() {
        if (folderInterests.isEmpty()) {
            return null;
        }
        return COMMA_JOINER.join(folderInterests);
    }

    public void setFolderInterests(String fInterests) {
        this.folderInterests.clear();
        for (String fi : COMMA_SPLITTER.split(Strings.nullToEmpty(fInterests))) {
            folderInterests.add(Integer.parseInt(fi));
        }
    }

    @XmlTransient
    public Set<Integer> getFolderInterestsAsSet() { return folderInterests; }

    public void setFolderInterests(Integer... folderIds) {
        this.folderInterests.clear();
        if (folderIds != null) {
            for (Integer folderId : folderIds) {
                this.folderInterests.add(folderId);
            }
        }
    }

    public void setFolderInterests(Collection<Integer> folderIds) {
        this.folderInterests.clear();
        if (folderIds != null) {
            this.folderInterests.addAll(folderIds);
        }
    }

    /**
     * @zm-api-field-tag waitset-folder-interests
     * @zm-api-field-description Comma separated list of IDs for folders.
     */
    @XmlAttribute(name=MailConstants.A_CHANGED_FOLDERS /* changedFolders */, required=false)
    public String getChangedFolders() {
        if (changedFolders.isEmpty()) {
            return null;
        }
        return COMMA_JOINER.join(changedFolders);
    }

    public void setChangedFolders(String fInterests) {
        this.changedFolders.clear();
        for (String fi : COMMA_SPLITTER.split(Strings.nullToEmpty(fInterests))) {
            changedFolders.add(Integer.parseInt(fi));
        }
    }

    @XmlTransient
    public Set<Integer> getChangedFoldersAsSet() { return changedFolders; }

    public void setChangedFolders(Integer... folderIds) {
        this.changedFolders.clear();
        if (folderIds != null) {
            for (Integer folderId : folderIds) {
                this.changedFolders.add(folderId);
            }
        }
    }

    public void setChangedFolders(Collection<Integer> folderIds) {
        this.changedFolders.clear();
        if (folderIds != null) {
            this.changedFolders.addAll(folderIds);
        }
    }
}
