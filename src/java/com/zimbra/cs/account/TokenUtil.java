/*
 * ***** BEGIN LICENSE BLOCK *****
 *
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.BlobMetaDataEncodingException;
import com.zimbra.cs.account.ZimbraAuthToken.ByteKey;

public class TokenUtil {

    public static Map<?, ?> getAttrs(String data) throws AuthTokenException{
        try {
            String decoded = new String(Hex.decodeHex(data.toCharArray()));
            return BlobMetaData.decode(decoded);
        } catch (DecoderException e) {
            throw new AuthTokenException("decoding exception", e);
        } catch (BlobMetaDataEncodingException e) {
            throw new AuthTokenException("blob decoding exception", e);
        }
    }

    public static String getHmac(String data, byte[] key) {
        try {
            ByteKey bk = new ByteKey(key);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(bk);
            return new String(Hex.encodeHex(mac.doFinal(data.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal error", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("fatal error", e);
        }
    }
}
