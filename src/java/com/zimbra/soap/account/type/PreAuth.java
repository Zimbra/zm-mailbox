/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;


/*
<preauth timestamp="{timestamp}" expires="{expires}">{computed-preauth-value}</preauth>
 */
public class PreAuth {

    /**
     * @zm-api-field-description Time stamp
     */
    @XmlAttribute(required=true) private long timestamp;
    /**
     * @zm-api-field-tag expires
     * @zm-api-field-description expiration time of the authtoken, in milliseconds. set to 0 to use the default
     * expiration time for the account. Can be used to sync the auth token expiration time with the external system's
     * notion of expiration (like a Kerberos TGT lifetime, for example).
     */
    @XmlAttribute private Long expiresTimestamp;
    /**
     * @zm-api-field-tag computed-preauth-value
     * @zm-api-field-description Computed preauth value
     */
    @XmlValue private String value;

    public long getTimestamp() { return timestamp; }
    public PreAuth setTimestamp(long timestamp) { this.timestamp = timestamp; return this; }

    public Long getExpiresTimestamp() { return expiresTimestamp; }
    public PreAuth setExpiresTimestamp(Long timestamp) { this.expiresTimestamp = timestamp; return this; }

    public String getValue() { return value; }
    public PreAuth setValue(String value) { this.value = value; return this; }
}
