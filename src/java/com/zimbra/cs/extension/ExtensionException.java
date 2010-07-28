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
