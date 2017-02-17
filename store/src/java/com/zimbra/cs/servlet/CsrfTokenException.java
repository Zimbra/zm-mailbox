/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

    /**
     * @param string
     * @param e
     */
    public CsrfTokenException(String message,Throwable cause) {
       super(message, cause);
    }

}
