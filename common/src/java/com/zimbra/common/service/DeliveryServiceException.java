/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.common.service;

@SuppressWarnings("serial")
public class DeliveryServiceException extends ServiceException {
    public static final String DELIVERY_REJECTED = "deliveryRejected.DELIVERY_REJECTED";

    private DeliveryServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }

    private DeliveryServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static DeliveryServiceException DELIVERY_REJECTED(String message, Throwable cause) {
        return new DeliveryServiceException("Message delivery refused, reason: " + message,
                                            DELIVERY_REJECTED, SENDERS_FAULT, cause);
    }
}
