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

package com.zimbra.cs.util.tnef;

import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;

import net.fortuna.ical4j.data.ContentHandler;

public interface TnefToICalendar {

    /**
     *
     * @param mimeMsg is the entire MIME message containing the TNEF winmail.dat attachment
     * @param tnefInput is an InputStream to the TNEF winmail.dat data.  It's not the entire MIME message.
     * @param icalOutput is the ical generator object.
     * @return true if the TNEF represented a Scheduling or Task related object that was converted
     *         successfully to ICAL in <code>icalOutput</code>
     * @throws ServiceException
     */
    public boolean convert(MimeMessage mimeMsg, InputStream tnefInput, ContentHandler icalOutput)
    throws ServiceException;
}
