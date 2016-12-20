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
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;

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

    private final List <Integer> folderIds = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountIdAndFolderIds() {
        this((String) null);
    }

    public AccountIdAndFolderIds(String id) {
        this.id = id;
    }

    public AccountIdAndFolderIds(Integer id) {
        this(id.toString());
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
}
