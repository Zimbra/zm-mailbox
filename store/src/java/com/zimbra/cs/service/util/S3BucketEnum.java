/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
package com.zimbra.cs.service.util;

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;

public enum S3BucketEnum {

    ACTIVE("ACTIVE"), DELETED("DELETED"), AWS_S3("AWS_S3"), OPENIO_S3("OPENIO_S3"), CEPH_S3("CEPH_S3"), NETAPP_S3(
            "NETAPP_S3"), HTTPS("HTTPS"), HTTP("HTTP");

    private String value;

    private S3BucketEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static S3BucketEnum fromString(String value) throws ServiceException {
        try {
            if (value != null) {
                return S3BucketEnum.valueOf(value);
            } else {
                return null;
            }
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(S3BucketEnum.values()), null);
        }
    }
}
