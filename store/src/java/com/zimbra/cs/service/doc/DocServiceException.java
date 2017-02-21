/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.doc;

import com.zimbra.common.service.ServiceException;

@SuppressWarnings("serial")
public class DocServiceException extends ServiceException {

    public static final String ERROR = "doc.ERROR";

    private DocServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    private DocServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }
    public static DocServiceException ERROR(String w) {
        return new DocServiceException("error: "+ w, ERROR, SENDERS_FAULT);
    }
    public static DocServiceException ERROR(String w, Throwable cause) {
        return new DocServiceException("error: "+ w, ERROR, SENDERS_FAULT, cause);
    }
    public static DocServiceException INVALID_PATH(String path) {
        return new DocServiceException("invalid path: "+ path, ERROR, SENDERS_FAULT);
    }
}
