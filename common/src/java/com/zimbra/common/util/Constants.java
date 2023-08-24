/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

/**
 * A place to keep commonly-used constants.
 * 
 * @author bburtin
 */
public class Constants {
    public static final long MILLIS_PER_SECOND = 1000;
    public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    public static final long MILLIS_PER_HOUR   = MILLIS_PER_MINUTE * 60;
    public static final long MILLIS_PER_DAY    = MILLIS_PER_HOUR * 24;
    public static final long MILLIS_PER_WEEK = MILLIS_PER_DAY * 7;
    public static final long MILLIS_PER_MONTH = MILLIS_PER_DAY * 31;

    public static final int SECONDS_PER_MINUTE = 60;
    public static final int SECONDS_PER_HOUR   = SECONDS_PER_MINUTE * 60;
    public static final int SECONDS_PER_DAY    = SECONDS_PER_HOUR * 24;
    public static final int SECONDS_PER_WEEK = SECONDS_PER_DAY * 7;
    public static final int SECONDS_PER_MONTH = SECONDS_PER_DAY * 31;

    public static final String CSRF_TOKEN = "X-Zimbra-Csrf-Token";
    public static final String ERROR_CODE_NO_SUCH_DOMAIN = "account.NO_SUCH_DOMAIN";

    public static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    public static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    public static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    public static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    //JWT constants
    public static final String TOKEN_VALIDITY_VALUE_CLAIM = "tvv";
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER= "Bearer";
    public static final String JWT_SALT_SEPARATOR = "|";
    public static final Integer LOCATOR_UPDATE_THREAD_COUNT  = 1;


}
