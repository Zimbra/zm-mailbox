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

    public enum Encoding {
        BASE32, BASE64;
    }
}
