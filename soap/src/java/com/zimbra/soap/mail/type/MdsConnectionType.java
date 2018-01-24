/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.base.Function;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.type.DataSource.ConnectionType;

@XmlEnum
public enum MdsConnectionType {
    @XmlEnumValue("cleartext") cleartext,
    @XmlEnumValue("ssl") ssl,
    @XmlEnumValue("tls") tls,
    @XmlEnumValue("tls_if_available") tls_if_available;

    public static MdsConnectionType fromString(String s) throws ServiceException {
        try {
            return MdsConnectionType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("invalid type: " + s + ", valid values: " +
                Arrays.asList(MdsConnectionType.values()), e);
        }
    }

    public static Function<ConnectionType, MdsConnectionType> CT_TO_MCT =
        new Function<ConnectionType, MdsConnectionType>() {
            @Override
            public MdsConnectionType apply(ConnectionType from) {
                switch (from) {
                case cleartext : return cleartext;
                case ssl: return ssl;
                case tls: return tls;
                case tls_if_available : return tls_if_available;
                }
                ZimbraLog.soap.warn("Unexpected connection type %s.  Returning %s.", from, cleartext);
                return cleartext;
            }
    };

    public static Function<MdsConnectionType, ConnectionType> MCT_TO_CT =
        new Function<MdsConnectionType, ConnectionType>() {
            @Override
            public ConnectionType apply(MdsConnectionType from) {
                switch (from) {
                case cleartext : return ConnectionType.cleartext;
                case ssl: return ConnectionType.ssl;
                case tls: return ConnectionType.tls;
                case tls_if_available : return ConnectionType.tls_if_available;
                }
                ZimbraLog.soap.warn("Unexpected connection type %s.  Returning %s.", from, ConnectionType.cleartext);
                return ConnectionType.cleartext;
            }
    };
}
