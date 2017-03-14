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
package com.zimbra.cs.zimlet;

/**
 * 
 * @author jylee
 *
 */
@SuppressWarnings("serial")
public class ZimletException extends Exception {

    private ZimletException(String msg) {
        super(msg);
    }

    private ZimletException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public static ZimletException ZIMLET_HANDLER_ERROR(String msg) {
        return new ZimletException(msg);
    }

    public static ZimletException INVALID_ZIMLET_DESCRIPTION(String msg) {
        return new ZimletException(msg);
    }

    public static ZimletException INVALID_ZIMLET_CONFIG(String msg) {
        return new ZimletException(msg);
    }

    public static ZimletException INVALID_ZIMLET_NAME() {
        return new ZimletException("Zimlet name may contain only letters, numbers and the following symbols: '.', '-' and '_'");
    }

    public static ZimletException INVALID_ZIMLET_NAME(String msg) {
        return new ZimletException(msg);
    }

    public static ZimletException INVALID_ZIMLET_ENTRY(String entry) {
        return new ZimletException(String.format("Invalid entry in Zimlet archive: %s", entry));
    }

    public static ZimletException INVALID_ABSOLUTE_PATH(String entry) {
        return new ZimletException(String.format("Invalid entry in Zimlet archive: %s. Zimlet entries with absolute paths are not allowed.", entry));
    }

    public static ZimletException CANNOT_DEPLOY(String zimlet, Throwable cause) {
        return new ZimletException("Cannot deploy Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_DEPLOY(String zimlet, String msg, Throwable cause) {
        return new ZimletException(String.format("Cannot deploy Zimlet %s. Error message: %s", zimlet, msg), cause);
    }

    public static ZimletException CANNOT_CREATE(String zimlet, Throwable cause) {
        return new ZimletException("Cannot create Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_DELETE(String zimlet, Throwable cause) {
        return new ZimletException("Cannot delete Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_ACTIVATE(String zimlet, Throwable cause) {
        return new ZimletException("Cannot activate Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_DEACTIVATE(String zimlet, Throwable cause) {
        return new ZimletException("Cannot deactivate Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_ENABLE(String zimlet, Throwable cause) {
        return new ZimletException("Cannot enable Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_DISABLE(String zimlet, Throwable cause) {
        return new ZimletException("Cannot disable Zimlet " + zimlet, cause);
    }

    public static ZimletException CANNOT_FLUSH_CACHE(Throwable cause) {
        return new ZimletException("Cannot flush zimlet cache", cause);
    }
}