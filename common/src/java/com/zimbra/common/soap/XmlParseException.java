/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.soap;

import com.zimbra.common.service.ServiceException;

public class XmlParseException
extends ServiceException {

    private static final long serialVersionUID = 2012769501847268691L;

    protected XmlParseException(String message) {
        super(message, PARSE_ERROR, SENDERS_FAULT);
    }

    /**
     * Note: very generic message used to provide data hiding.
     */
    public static XmlParseException PARSE_ERROR() {
        return new XmlParseException("Document parse failed");
    }
}
