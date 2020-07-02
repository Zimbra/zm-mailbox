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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ItemSpec;

import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get item
 * <br />
 * A successful GetItemResponse will contain a single element appropriate for the type of the requested item if there
 * is no matching item, a fault containing the code mail.NO_SUCH_ITEM is returned
 * @zm-api-request-description The caller must specify one of:
 * <ul>
 * <li> an {item-id},
 * <li> a fully-qualified {path}, or
 * <li> both a {folder-id} and a relative {path}
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_ITEM_REQUEST)
public class GetItemRequest {

    /**
     * @zm-api-field-description Item specification
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_ITEM /* item */, required=false)
    private ItemSpec item;

    public GetItemRequest() {
    }

    public void setItem(ItemSpec item) { this.item = item; }
    public ItemSpec getItem() { return item; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("item", item);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
