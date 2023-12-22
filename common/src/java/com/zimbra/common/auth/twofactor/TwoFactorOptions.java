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
package com.zimbra.common.auth.twofactor;

import com.zimbra.common.service.ServiceException;

public class TwoFactorOptions {

    public enum HashAlgorithm {
        SHA1("HmacSHA1"),
        SHA256("HmacSHA256"),
        SHA512("HmacSHA512");

        private String algorithm;

        HashAlgorithm(String lookupLabel) {
            this.algorithm = lookupLabel;
        }

        public String getLabel() {
            return algorithm;
        }
    }

    public enum CodeLength {
        SIX(6), EIGHT(8);

        private int num;

        private CodeLength(int n) {
            this.num = n;
        }

        public int getValue() {
            return num;
        }

        public static CodeLength valueOf(int n) throws ServiceException {
            for (CodeLength l: CodeLength.values()) {
                if (l.getValue() == n) {
                    return l;
                }
            }
            throw ServiceException.FAILURE(String.format("%s is not a valid two-factor code length. Possible values are %s", n, CodeLength.values()), new Throwable());
        }
    }

    public enum EmailCodeLength {
        FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9);

        private int num;

        private EmailCodeLength(int n) {
            this.num = n;
        }

        public int getValue() {
            return num;
        }

        public static EmailCodeLength valueOf(int n) throws ServiceException {
            for (EmailCodeLength l: EmailCodeLength.values()) {
                if (l.getValue() == n) {
                    return l;
                }
            }
            throw ServiceException.FAILURE(String.format("%s is not a valid two-factor email code length. Possible values are %s", n, EmailCodeLength.values()), new Throwable());
        }
    }

    public enum Encoding {
        BASE32, BASE64;
    }
}
