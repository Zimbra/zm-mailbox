/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.IMAPItemInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_IMAP_COPY_RESPONSE)
public class IMAPCopyResponse {
    /**
     * @zm-api-field-description new items
     */
    @XmlElements(
        @XmlElement(name=MailConstants.E_ITEM /* m */, type=IMAPItemInfo.class)
    )
    private final List<IMAPItemInfo> newItems = Lists.newArrayList();

    public IMAPCopyResponse() {
    }

    public void setItems(Iterable <IMAPItemInfo> item) {
        this.newItems.clear();
        Iterables.addAll(this.newItems,item);
    }

    public IMAPCopyResponse addItem(IMAPItemInfo item) {
        this.newItems.add(item);
        return this;
    }

    public List<IMAPItemInfo> getItems() {
        return newItems;
    }
}
