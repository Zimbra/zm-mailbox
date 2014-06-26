/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.type;

import java.util.Arrays;
import java.util.Locale;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;

/**
 * relates to Logging levels.
 */
@XmlEnum
public enum LoggingLevel {
    // keep in sync with com.zimbra.common.util.Log.Level
    error, warn, info, debug, trace;

    public static LoggingLevel fromString(String s) throws ServiceException {
        try {
            return LoggingLevel.valueOf(s.toLowerCase(Locale.ENGLISH));
        } catch (IllegalArgumentException iae) {
            throw ServiceException.INVALID_REQUEST("unknown Logging Level: " + s + ", valid values: " +
                            Arrays.asList(LoggingLevel.values()), null);
        }
    }
}
