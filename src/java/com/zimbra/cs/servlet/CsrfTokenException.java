/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.servlet;


/**
 * @author zimbra
 *
 */
public class CsrfTokenException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -3966730476787360251L;

    /**
     *
     */
    public CsrfTokenException() {
        super();
    }

    /**
     * @param message
     */
    public CsrfTokenException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public CsrfTokenException(Throwable cause) {
        super(cause);
    }

}
