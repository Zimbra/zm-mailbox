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
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.NameId;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_DELETE_DEVICE_RESPONSE)
public class DeleteDeviceResponse {

    /**
     * @zm-api-field-description Information about deleted device
     */
    @XmlElement(name=MailConstants.E_DEVICE /* device */, required=true)
    private final NameId device;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DeleteDeviceResponse() {
        this((NameId) null);
    }

    public DeleteDeviceResponse(NameId device) {
        this.device = device;
    }

    public static DeleteDeviceResponse fromId(String id) {
        NameId dev = new NameId(null, id);
        return new DeleteDeviceResponse(dev);
    }

    public NameId getDevice() { return device; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("device", device);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
