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

package com.zimbra.soap.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;

@XmlEnum
public enum LicenseStatus {
    // case must match protocol
    NOT_INSTALLED, NOT_ACTIVATED, IN_FUTURE, EXPIRED, INVALID, LICENSE_GRACE_PERIOD, ACTIVATION_GRACE_PERIOD, OK;

    public static LicenseStatus fromString(String s) throws ServiceException {
        try {
            return LicenseStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("Invalid license status: " + s +
                    ", valid values: " +
                    Arrays.asList(LicenseStatus.values()), null);
        }
    }
}
