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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DismissAlarm {

    /**
     * @zm-api-field-tag cal-item-id
     * @zm-api-field-description Calendar item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag dismissed-at-millis
     * @zm-api-field-description Time alarm was dismissed, in millis
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALARM_DISMISSED_AT /* dismissedAt */, required=true)
    private final long dismissedAt;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected DismissAlarm() {
        this((String) null, -1L);
    }

    public DismissAlarm(String id, long dismissedAt) {
        this.id = id;
        this.dismissedAt = dismissedAt;
    }

    public String getId() { return id; }
    public long getDismissedAt() { return dismissedAt; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("dismissedAt", dismissedAt);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
