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
package com.zimbra.cs.filter.jsieve;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.google.common.collect.Sets;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public abstract class CommunityAbstractTest extends AbstractTest {

    protected static final String notificationTypeHeaderName = "X-Zimbra-Community-Notification-Type";

    protected static final HashSet<String> requestNotifications = Sets.newHashSet(
            "bb196c30-fad3-4ad8-a644-2a0187fc5617", //       Friendships Requests
            "3772ef23-6aa0-4a22-9e88-a4313c37ebe6", //       Group Membership Request
            "1b0500d2-c789-421e-a25b-3ab823af53be"  //       Forums Requiring Moderation
        );

    protected static final HashSet<String> contentNotifications = Sets.newHashSet(
            "6a3659db-dec2-477f-981c-ada53603ccbb", //       Likes
            "94dc0d37-3a65-43de-915d-d7d62774b576", //       Ratings
            "d0e2bf58-74b3-4090-8ff0-0d9a11188f0b", //       Also Commented
            "95a4065f-1177-4dca-b8d5-a27d4d0d7b63", //       Blog Post Comment
            "5c976a15-64a5-4974-a5d6-a247163f2e01", //       Blog Post
            "5898644c-b6a0-4d82-a4c6-6e5a8940884c", //       Content Author Comments
            "4876d7ce-b48c-4a08-8cd9-872342c5bdf8", //       Wiki Page Comment
            "82e1d0b4-854e-43c9-85d7-dea7d4dec949", //       Wiki Page Update
            "8e627c29-8602-4110-877d-0232e4ea2fd5", //       Media Comment
            "be997a7a-5026-435f-8ea5-4fe3d90a6ba9", //       New Media in Owned Gallery
            "eea4ccbb-6e07-4a6d-9bb6-8b02f060f79c", //       Forum Thread Awaiting Moderation
            "352a702d-2a77-4307-9e9e-c564426e8cc8", //       Forum Thread Subscription
            "0e952633-fa46-448d-b1aa-bb6c60a388fb", //       Forum Reply Awaiting Moderation
            "e3df1b21-ac81-4eb3-8ab6-69dc049f5684", //       Forum Replies
            "f8c93cd5-d40e-461d-b13a-b02e92bfcbbf"  //       Forum Thread Verified Answers
        );

    protected static final HashSet<String> connectionsNotifications = Sets.newHashSet(
            "194d3363-f5a8-43b4-a1bd-92a95f6dd76b", //       Friendships
            "1cee0f92-3650-4110-9f0b-69b0e175914d", //       Mentions
            "16828d04-0555-4c80-88c4-da87e86698b8", //       Group Membership
            "84e4c0a7-3a17-4dfe-b8a0-d7038aa04a84", //       Private Messages
            "df527fff-4d37-4b8c-afbc-949b6337cdca", //       Follows
            "328e5139-d759-405c-98da-91cd25bcc80c"  //       Group Mentions
            );

    protected boolean checkHeaderValue(MailAdapter mail, Set<String> values) {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;
        List<String> header = adapter.getHeader(notificationTypeHeaderName);
        if (!header.isEmpty()) {
            for (String id: header) {
                if (values.contains(id)) {
                    return true;
                }
            }
        }
        return false;
    }
}
