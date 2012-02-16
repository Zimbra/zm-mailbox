/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ItemSpec;

/**
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
    @XmlElement(name=MailConstants.E_ITEM /* item */, required=false)
    private ItemSpec item;

    public GetItemRequest() {
    }

    public void setItem(ItemSpec item) { this.item = item; }
    public ItemSpec getItem() { return item; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("item", item);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
