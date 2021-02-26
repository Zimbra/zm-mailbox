/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class SyncConstants {
    public static final String NAMESPACE_STR = "urn:zimbraSync";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_GET_DEVICE_STATUS_REQUEST = "GetDeviceStatusRequest";
    public static final String E_GET_DEVICE_STATUS_RESPONSE = "GetDeviceStatusResponse";
    public static final String E_REMOVE_DEVICE_REQUEST = "RemoveDeviceRequest";
    public static final String E_REMOVE_DEVICE_RESPONSE = "RemoveDeviceResponse";
    public static final String E_SUSPEND_DEVICE_REQUEST = "SuspendDeviceRequest";
    public static final String E_SUSPEND_DEVICE_RESPONSE = "SuspendDeviceResponse";
    public static final String E_RESUME_DEVICE_REQUEST = "ResumeDeviceRequest";
    public static final String E_RESUME_DEVICE_RESPONSE = "ResumeDeviceResponse";
    public static final String E_REMOTE_WIPE_REQUEST = "RemoteWipeRequest";
    public static final String E_REMOTE_WIPE_RESPONSE = "RemoteWipeResponse";
    public static final String E_CANCEL_PENDING_REMOTE_WIPE_REQUEST = "CancelPendingRemoteWipeRequest";
    public static final String E_CANCEL_PENDING_REMOTE_WIPE_RESPONSE = "CancelPendingRemoteWipeResponse";

    public static final QName GET_DEVICE_STATUS_REQUEST = QName.get(E_GET_DEVICE_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_DEVICE_STATUS_RESPONSE = QName.get(E_GET_DEVICE_STATUS_RESPONSE, NAMESPACE);
    public static final QName REMOVE_DEVICE_REQUEST = QName.get(E_REMOVE_DEVICE_REQUEST, NAMESPACE);
    public static final QName REMOVE_DEVICE_RESPONSE = QName.get(E_REMOVE_DEVICE_RESPONSE, NAMESPACE);
    public static final QName SUSPEND_DEVICE_REQUEST = QName.get(E_SUSPEND_DEVICE_REQUEST, NAMESPACE);
    public static final QName SUSPEND_DEVICE_RESPONSE = QName.get(E_SUSPEND_DEVICE_RESPONSE, NAMESPACE);
    public static final QName RESUME_DEVICE_REQUEST = QName.get(E_RESUME_DEVICE_REQUEST, NAMESPACE);
    public static final QName RESUME_DEVICE_RESPONSE = QName.get(E_RESUME_DEVICE_RESPONSE, NAMESPACE);
    public static final QName REMOTE_WIPE_REQUEST = QName.get(E_REMOTE_WIPE_REQUEST, NAMESPACE);
    public static final QName REMOTE_WIPE_RESPONSE = QName.get(E_REMOTE_WIPE_RESPONSE, NAMESPACE);
    public static final QName CANCEL_PENDING_REMOTE_WIPE_REQUEST = QName.get(E_CANCEL_PENDING_REMOTE_WIPE_REQUEST, NAMESPACE);
    public static final QName CANCEL_PENDING_REMOTE_WIPE_RESPONSE = QName.get(E_CANCEL_PENDING_REMOTE_WIPE_RESPONSE, NAMESPACE);

    public static final String E_DEVICE = "device";
    public static final String E_PROVISIONABLE = "provisionable";
    public static final String E_STATUS = "status";
    public static final String E_FIRST_REQ_RECEIVED = "firstReqReceived";
    public static final String E_LAST_POLICY_UPDATE = "lastPolicyUpdate";
    public static final String E_REMOTE_WIPE_REQ_TIME = "remoteWipeReqTime";
    public static final String E_REMOTE_WIPE_ACK_TIME = "remoteWipeAckTime";
    public static final String E_RECOVERY_PASSWORD = "recoveryPassword";
    public static final String E_LAST_USED_DATE = "lastUsedDate";
    public static final String E_FOLDER = "folder";
    public static final String E_SYNCSTATE = "syncState";
    public static final String E_ITEMS = "item";
    public static final String E_MAILBOX = "mailbox";
    public static final String E_DEVICE_NAME = "deviceName";
    public static final String E_DEVICE_TYPE = "deviceType";
    public static final String E_DEVICE_LAST_USED = "deviceLastUsed";
    public static final String E_DEVICE_SYNC_VERSION = "deviceSyncVersion";
    public static final String E_EMAIL_ADDRESS = "emailAddress";

    public static final String A_ID = "id";
    public static final String A_TYPE = "type";
    public static final String A_UA = "ua";
    public static final String A_PROTOCOL = "protocol";
    public static final String A_MODEL = "model";
    public static final String A_IMEI = "imei";
    public static final String A_FRIENDLYNAME = "friendly_name";
    public static final String A_OS = "os";
    public static final String A_OSLANGUAGE = "os_language";
    public static final String A_PHONENUMBER = "phone_number";
    public static final String A_CLASS = "class";
    public static final String A_SHOWITEM = "showItems";
    public static final String A_OFFSET = "offset";
    public static final String A_LIMIT = "limit";
    public static final String A_FILTERDEVICESBYAND = "filterDevicesByAnd";

    // Sync command response statuses
    // Reference - https://msdn.microsoft.com/en-us/library/gg675457(v=exchg.80).aspx
    public static final Integer STATUS_SUCCESS = 1;
    public static final Integer STATUS_INVALID_SYNC_KEY = 3;
    public static final Integer STATUS_PROTOCOL_ERROR = 4;
    public static final Integer STATUS_SERVER_ERROR = 5;
    public static final Integer STATUS_ERROR_IN_CONVERSATION = 6;
    public static final Integer STATUS_CONFLICT = 7;
    public static final Integer STATUS_OBJECT_NOT_FOUND = 8;
    public static final Integer STATUS_SYNC_NOT_COMPLETED = 9;
    public static final Integer STATUS_FOLDER_HIERARCHY_CHANGED = 12;
    public static final Integer STATUS_EMPTY_OR_PARTIAL_SYNC = 13;
    public static final Integer STATUS_INVALID_INTERVAL = 14;
    public static final Integer STATUS_INVALID_SYNC_REQUEST = 15;
    public static final Integer STATUS_SYNC_RETRY = 16;

    // ItemOperation response statuses
    // Reference - https://msdn.microsoft.com/en-us/library/gg663459(v=exchg.80).aspx
    public static final Integer ITEMOPERATIONS_STATUS_SUCCESS = 1;
    public static final Integer ITEMOPERATIONS_STATUS_PROTOCOL_ERROR = 2;
    public static final Integer ITEMOPERATIONS_STATUS_SERVER_ERROR = 3;
    public static final Integer ITEMOPERATIONS_STATUS_DOCUMENT_LIB_BAD_URI = 4;
    public static final Integer ITEMOPERATIONS_STATUS_DOCUMENT_LIB_ACCESS_DENIED = 5;
    public static final Integer ITEMOPERATIONS_STATUS_DOCUMENT_LIB_OBJECT_NOT_FOUND = 6;
    public static final Integer ITEMOPERATIONS_STATUS_DOCUMENT_LIB_CONNECTION_ERROR = 7;
    public static final Integer ITEMOPERATIONS_STATUS_INVALID_BYTE_RANGE = 8;
    public static final Integer ITEMOPERATIONS_STATUS_UNKNOWN_STORE = 9;
    public static final Integer ITEMOPERATIONS_STATUS_EMPTY_FILE = 10;
    public static final Integer ITEMOPERATIONS_STATUS_DATA_SIZE_TOO_LARGE = 11;
    public static final Integer ITEMOPERATIONS_STATUS_IO_FAILURE = 12;
    public static final Integer ITEMOPERATIONS_STATUS_ITEM_CONVERSION_FAILURE = 14;
    public static final Integer ITEMOPERATIONS_STATUS_ATTACHMENT_INVALID = 15;
    public static final Integer ITEMOPERATIONS_STATUS_RESOURCE_ACCESS_DENIED = 16;
    public static final Integer ITEMOPERATIONS_STATUS_PARTIAL_SUCCESS = 17;
    public static final Integer ITEMOPERATIONS_STATUS_CREDENTIALS_REQUIRED = 18;
    public static final Integer ITEMOPERATIONS_STATUS_ELEMENT_MISSING = 155;
    public static final Integer ITEMOPERATIONS_STATUS_ACTION_NOT_SUPPORTED = 156;
}
