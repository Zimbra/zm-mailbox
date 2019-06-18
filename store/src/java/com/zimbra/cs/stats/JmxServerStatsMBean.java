/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.stats;

public interface JmxServerStatsMBean extends JmxStatsMBeanBase
{
    long getBlobInputStreamReads();
    long getBlobInputStreamSeekRate();
    long getDatabaseConnectionGets();
    long getDatabaseConnectionGetMs();
    long getDatabaseConnectionsInUse();
    long getImapRequests();
    long getImapResponseMs();
    long getItemCacheHitRate();
    long getLdapDirectoryContextGetMs();
    long getLdapDirectoryContextGets();
    long getLmtpDeliveredBytes();
    long getLmtpDeliveredMessages();
    long getLmtpReceivedBytes();
    long getLmtpReceivedMessages();
    long getLmtpRecipients();
    long getMailboxCacheHitRate();
    long getMailboxCacheSize();
    long getMailboxGetMs();
    long getMailboxGets();
    long getMessageAddMs();
    long getMessageCacheSize();
    long getMessageCacheHitRate();
    long getMessagesAdded();
    long getPopRequests();
    long getPopResponseMs();
    long getSoapRequests();
    long getSoapResponseMs();
    long getStoreCopys();
    long getStoreDeletes();
    long getStoreGets();
    long getStoreLinks();
    long getStorePuts();
    long getStoreStages();
    long getStoreCopyMs();
    long getStoreDeleteMs();
    long getStoreGetMs();
    long getStoreLinkMs();
    long getStorePutMs();
    long getStoreStageMs();
}
