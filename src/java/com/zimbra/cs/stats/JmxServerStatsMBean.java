/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.stats;

public interface JmxServerStatsMBean
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
}
