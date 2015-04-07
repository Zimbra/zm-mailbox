/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.amqp;

import org.springframework.amqp.core.DirectExchange;

public interface AmqpConstants {

    /** A ChangeNotification changeId */
    String HEADER_CHANGE_ID = "changeId";

    /** The mailbox operation that was performed */
    String HEADER_MAILBOX_OP = "op";

    /** The Server ID of the sender */
    String HEADER_SENDER_SERVER_ID = "senderServerId";

    /** The timestamp of when the operation was performed */
    String HEADER_TIMESTAMP = "timestamp";

    DirectExchange EXCHANGE_MBOX = new DirectExchange("zimbra.mbox", false /** durable=no */, false /** auto-delete=no */);
}
