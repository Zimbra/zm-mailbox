/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.extension;

/**
 * This exception is thrown by Zimbra extension classes when they notify
 * an extension specific error to Zimbra extension framework.
 * <p>
 * For example, {@link ZimbraExtension#init()} may throw this exception when it
 * failed its initialization and wants to unregister the extension from the
 * framework.
 *
 * @author ysasaki
 */
public class ExtensionException extends Exception {
    private static final long serialVersionUID = 3703802218451911403L;

    public ExtensionException(String message) {
        super(message);
    }

    public ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

}
