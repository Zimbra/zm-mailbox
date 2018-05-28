/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.BlobMetaDataEncodingException;
import com.zimbra.cs.account.AuthTokenKey;
import com.zimbra.cs.account.DataSource;

public class JWEUtil {
    public static String getJWE(Map <String, String> map) throws ServiceException {
        String encryptedData = null;
        if (map == null) {
            return encryptedData;
        }
        AuthTokenKey key = AuthTokenKey.getCurrentKey();
        StringBuilder encodedBuff = new StringBuilder(64);
        map.entrySet().forEach(e -> BlobMetaData.encodeMetaData(e.getKey(), e.getValue(), encodedBuff));
        encryptedData = key.getVersion() + "_" + DataSource.encryptData(new String(key.getKey()), encodedBuff.toString());
        return encryptedData;
    }

    public static Map <String, String> getDecodedJWE(String jwe) throws ServiceException {
        Map<String, String> result = null;
        if (StringUtils.isEmpty(jwe)) {
            return result;
        }
        String[] jweArr = jwe.split("_");
        if (jweArr.length != 2) {
            throw ServiceException.PARSE_ERROR("invalid jwe format", null);
        }
        AuthTokenKey key = AuthTokenKey.getVersion(jweArr[0]);
        String data = DataSource.decryptData(new String(key.getKey()), jweArr[1]);
        try {
            Map <?,?> map = BlobMetaData.decode(data);
            result = map.entrySet().stream()
                    .collect(Collectors.toMap( e -> (String) e.getKey(), e -> (String)e.getValue()));
        } catch (BlobMetaDataEncodingException e) {
            throw ServiceException.FAILURE("failed to get decoded jwe", e);
        }

        return result;
    }
}
