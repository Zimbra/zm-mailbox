/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CacheEntrySelector {

    public static enum CacheEntryBy {

        // case must match protocol
        id, name;

        public static CacheEntryBy fromString(String s) throws ServiceException {
            try {
                return CacheEntryBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }

        public com.zimbra.common.account.Key.CacheEntryBy toKeyCacheEntryBy()
        throws ServiceException {
            return com.zimbra.common.account.Key.CacheEntryBy.fromString(this.name());
        }
    }

    /**
     * @zm-api-field-tag cache-entry-key
     * @zm-api-field-description The key used to identify the cache entry. Meaning determined by <b>{cache-entry-by}</b>
     */
    @XmlValue private final String key;

    /**
     * @zm-api-field-tag cache-entry-by
     * @zm-api-field-description Select the meaning of <b>{cache-entry-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY) private final CacheEntryBy cacheEntryBy;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CacheEntrySelector() {
        this(null, null);
    }

    public CacheEntrySelector(CacheEntryBy by, String key) {
        this.cacheEntryBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public CacheEntryBy getBy() { return cacheEntryBy; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
                .add("cacheEntryBy", cacheEntryBy)
                .add("key", key);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
