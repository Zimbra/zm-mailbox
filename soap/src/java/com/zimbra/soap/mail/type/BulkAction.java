/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class BulkAction {

    @XmlEnum
    public static enum Operation {
        move, read, unread;

        public static Operation fromString(String s) throws ServiceException {
            try {
                return Operation.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown operation: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation to perform
     * <table>
     * <tr><td><b>move</b>           </td><td>move the search result to specified folder location</td></tr>
     * <tr><td><b>read</b>           </td><td>mark the search result as read</td></tr>
     * <tr><td><b>unread</b>         </td><td>mark the search result as unread</td></tr>
     * </table>
     */
    @XmlAttribute(name = MailConstants.A_OPERATION /* op */, required = true)
    private Operation op;

    /**
     * @zm-api-field-tag folder-path
     * @zm-api-field-description Required if op="move". Folder pathname where
     *                           all matching items should be moved.
     */
    @XmlAttribute(name = MailConstants.A_FOLDER /* l */, required = false)
    private String folder;

    public Operation getOp() {
        return op;
    }

    public void setOp(Operation op) {
        this.op = op;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
