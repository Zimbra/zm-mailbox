/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;

/**
 * Mailbox for accounts with zimbraIsExternalVirtualAccount set to TRUE.
 */
public class ExternalVirtualMailbox extends Mailbox {

    protected ExternalVirtualMailbox(MailboxData data) {
        super(data);
    }

    @Override
    public MailSender getMailSender() throws ServiceException {
        throw ServiceException.PERM_DENIED("permission denied for external account");
    }
}
