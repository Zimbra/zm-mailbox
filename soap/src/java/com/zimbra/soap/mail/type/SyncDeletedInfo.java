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

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class SyncDeletedInfo {

    /**
     * @zm-api-field-tag deleted-ids
     * @zm-api-field-description IDs of deleted items
     */
    @XmlAttribute(name=MailConstants.A_IDS /* ids */, required=true)
    private final String ids;

    /**
     * @zm-api-field-description Details of deletes broken down by item type (present if "typed" was specified
     * in the request)
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER /* folder */, type=FolderIdsAttr.class),
        @XmlElement(name=MailConstants.E_SEARCH /* search */, type=SearchFolderIdsAttr.class),
        @XmlElement(name=MailConstants.E_MOUNT /* link */, type=MountIdsAttr.class),
        @XmlElement(name=MailConstants.E_TAG /* tag */, type=TagIdsAttr.class),
        @XmlElement(name=MailConstants.E_CONV /* c */, type=ConvIdsAttr.class),
        @XmlElement(name=MailConstants.E_CHAT /* chat */, type=ChatIdsAttr.class),
        @XmlElement(name=MailConstants.E_MSG /* m */, type=MsgIdsAttr.class),
        @XmlElement(name=MailConstants.E_CONTACT /* cn */, type=ContactIdsAttr.class),
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=ApptIdsAttr.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=TaskIdsAttr.class),
        @XmlElement(name=MailConstants.E_NOTES /* notes */, type=NoteIdsAttr.class),
        @XmlElement(name=MailConstants.E_WIKIWORD /* w */, type=WikiIdsAttr.class),
        @XmlElement(name=MailConstants.E_DOC /* doc */, type=DocIdsAttr.class)
    })
    private List<IdsAttr> types = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SyncDeletedInfo() {
        this((String) null);
    }

    public SyncDeletedInfo(String ids) {
        this.ids = ids;
    }

    public void setTypes(Iterable <IdsAttr> types) {
        this.types.clear();
        if (types != null) {
            Iterables.addAll(this.types,types);
        }
    }

    public SyncDeletedInfo addType(IdsAttr type) {
        this.types.add(type);
        return this;
    }

    public String getIds() { return ids; }
    public List<IdsAttr> getTypes() {
        return Collections.unmodifiableList(types);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("ids", ids)
            .add("types", types);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)) .toString();
    }
}
