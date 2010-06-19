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
