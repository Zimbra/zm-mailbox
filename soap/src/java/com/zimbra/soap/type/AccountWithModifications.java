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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.PendingFolderModifications;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_A)
public class AccountWithModifications {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    private final String id;

    /**
     * @zm-api-field-tag change-id
     * @zm-api-field-description ID of the last change
     */
    @XmlAttribute(name=MailConstants.A_CHANGE_ID, required=false)
    private final int lastChangeId;

    /**
     * @zm-api-field-tag mods
     * @zm-api-field-description serialized pending modifications per folder
     * TODO: instead of a string this should be a structure that contains enough data to instantiate PendingRemoteModifications
     */
    @XmlElement(name=MailConstants.E_PENDING_FOLDER_MODIFICATIONS /* mod */, required=false)
    private Collection<PendingFolderModifications> mods;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountWithModifications() {
        this((String) null, (Collection<PendingFolderModifications>)null, 0);
    }

    public AccountWithModifications(String id, int lastChangeId) {
        this(id, null, lastChangeId);
    }

    public AccountWithModifications(Integer id) {
        this(id.toString(), null, 0);
    }

    public AccountWithModifications(String id, Collection<PendingFolderModifications> mods, int lastChangeId) {
        this.id = id;
        this.mods = mods;
        this.lastChangeId = lastChangeId;
    }

    public AccountWithModifications(Integer id, Collection<PendingFolderModifications> mods, int lastChangeId) {
        this(id.toString(), mods, lastChangeId);
    }

    public String getId() { return id; }
    public int getLastChangeId() { return lastChangeId; }
    public Collection<PendingFolderModifications> getPendingFolderModifications() {
        return mods;
    }

    public void setPendingFolderModifications(Collection<PendingFolderModifications> mods) {
        this.mods = mods;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("lastChangeId", lastChangeId)
            .add("mods", mods);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
