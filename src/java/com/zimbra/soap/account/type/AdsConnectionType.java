/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.base.Function;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.type.DataSource.ConnectionType;

@XmlEnum
public enum AdsConnectionType {
    @XmlEnumValue("cleartext") cleartext,
    @XmlEnumValue("ssl") ssl,
    @XmlEnumValue("tls") tls,
    @XmlEnumValue("tls_is_available") tls_if_available;

    public static AdsConnectionType fromString(String s) throws ServiceException {
        try {
            return AdsConnectionType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("invalid type: " + s + ", valid values: " +
                Arrays.asList(AdsConnectionType.values()), e); 
        }
    }

    public static Function<ConnectionType, AdsConnectionType> CT_TO_ACT =
        new Function<ConnectionType, AdsConnectionType>() {
            @Override
            public AdsConnectionType apply(ConnectionType from) {
                if (from == null) {
                    return null;
                }
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
    
    public static Function<AdsConnectionType, ConnectionType> ACT_TO_CT =
        new Function<AdsConnectionType, ConnectionType>() {
            @Override
            public ConnectionType apply(AdsConnectionType from) {
                if (from == null) {
                    return null;
                }
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