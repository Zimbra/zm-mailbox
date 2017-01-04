/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.type;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.PendingAccountModifications;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountIdAndFolderIds {

    private static Joiner COMMA_JOINER = Joiner.on(",");
    private static Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    private final String id;

    /**
     * @zm-api-field-tag mod
     * @zm-api-field-description serialized pending modifications
     * TODO: instead of a string this should be a structure that contains enough data to instantiate PendingRemoteModifications 
     */
    @XmlElement(name=MailConstants.E_PENDING_MODIFICATIONS /* mod */, required=false)
    private final PendingAccountModifications mod;

    private final List <Integer> folderIds = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountIdAndFolderIds() {
        this((String) null);
    }

    public AccountIdAndFolderIds(String id, PendingAccountModifications mods) {
        this.id = id;
        this.mod = mods;
    }

    public AccountIdAndFolderIds(Integer id, PendingAccountModifications mods) {
        this(id.toString(), mods);
    }

    public AccountIdAndFolderIds(String id) {
        this(id, (PendingAccountModifications)null);
    }

    public AccountIdAndFolderIds(Integer id) {
        this(id.toString(), (PendingAccountModifications)null);
    }

    public String getId() { return id; }

    /**
     * @zm-api-field-tag folder-ids
     * @zm-api-field-description Comma separated list of IDs for folders.
     */
    @XmlAttribute(name=AdminConstants.A_FOLDER_IDS /* folderIds */, required=false)
    public String getFolderIds() {
        if (folderIds.isEmpty()) {
            return null;
        }
        return COMMA_JOINER.join(folderIds);
    }

    @XmlTransient
    public List<Integer> getFolderIdsAsList() { return folderIds; }

    public void setFolderIds(String fInterests) {
        this.folderIds.clear();
        for (String fi : COMMA_SPLITTER.split(Strings.nullToEmpty(fInterests))) {
            folderIds.add(Integer.parseInt(fi));
        }
    }

    public AccountIdAndFolderIds setFolderIds(Integer... folderIds) {
        this.folderIds.clear();
        if (folderIds != null) {
            for (Integer folderId : folderIds) {
                this.folderIds.add(folderId);
            }
        }
        return this;
    }

    public AccountIdAndFolderIds setFolderIds(Collection<Integer> folderIds) {
        this.folderIds.clear();
        if (folderIds != null) {
            this.folderIds.addAll(folderIds);
        }
        return this;
    }

    public PendingAccountModifications getMods() {
        return mod;
    }
}
