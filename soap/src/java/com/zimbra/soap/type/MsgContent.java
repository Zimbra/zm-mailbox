/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.gql.GqlConstants;

import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.types.GraphQLType;


/**
 * Message Content the client expects in response
 *
 */
@XmlEnum
@GraphQLType(name=GqlConstants.CLASS_MESSAGE_CONTENT, description="Message content the cient expects in response")
public enum MsgContent {

    @GraphQLEnumValue(description="The complete message")
    full, // The complete message
    @GraphQLEnumValue(description="Only the Message and not quoted text")
    original, // Only the Message and not quoted text
    @GraphQLEnumValue(description="The complete message and also this message without quoted text")
    both; // The complete message and also this message without quoted text

    public static MsgContent fromString(String msgContent) {
        try {
            if (msgContent != null)
                return MsgContent.valueOf(msgContent);
            else
                return null;
        } catch (final Exception e) {
            return null;
        }
    }
}

