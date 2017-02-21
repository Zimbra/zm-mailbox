/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import com.zimbra.common.service.ServiceException;

public class ImapServiceException extends ServiceException {
    private static final long serialVersionUID = -8493068960356614305L;

    public static final String CANT_DELETE_SYSTEM_FOLDER = "imap.CANNOT_DELETE_SYSTEM_FOLDER";
    public static final String CANT_RENAME_INBOX = "imap.CANNOT_RENAME_INBOX";
    public static final String FOLDER_NOT_VISIBLE = "imap.FOLDER_NOT_VISIBLE";
    public static final String FOLDER_NOT_WRITABLE = "imap.FOLDER_NOT_WRITABLE";

    public static final String FOLDER_NAME = "folderName";

    private ImapServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    
//  private ImapServiceException(String message, String code, boolean isReceiversFault, Throwable cause, Argument... args) {
//      super(message, code, isReceiversFault, cause, args);
//  }

    public static ImapServiceException CANNOT_DELETE_SYSTEM_FOLDER(String folderName) {
        return new ImapServiceException("cannot delete system folder: ", CANT_DELETE_SYSTEM_FOLDER, SENDERS_FAULT, new Argument(FOLDER_NAME, folderName, Argument.Type.STR));
    }

    public static ImapServiceException CANT_RENAME_INBOX() {
        return new ImapServiceException("Rename of INBOX not supported", CANT_RENAME_INBOX, SENDERS_FAULT);
    }

    public static ImapServiceException FOLDER_NOT_VISIBLE(String folderName) {
        return new ImapServiceException("folder not visible: ", FOLDER_NOT_VISIBLE, SENDERS_FAULT, new Argument(FOLDER_NAME, folderName, Argument.Type.STR)); 
    }

    public static ImapServiceException FOLDER_NOT_WRITABLE(String folderName) {
        return new ImapServiceException("folder is READ-ONLY: ", FOLDER_NOT_WRITABLE, SENDERS_FAULT, new Argument(FOLDER_NAME, folderName, Argument.Type.STR)); 
    }
}
