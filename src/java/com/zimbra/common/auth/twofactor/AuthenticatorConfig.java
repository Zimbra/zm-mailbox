package com.zimbra.common.auth.twofactor;

import com.zimbra.common.auth.twofactor.AuthenticatorConfig.CodeLength;
import com.zimbra.common.service.ServiceException;

public class AuthenticatorConfig {
    private HashAlgorithm hashAlgorithm;
    private CodeLength codeLength;
    private long secondsInTimeWindow;
    private int allowedOffset;

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

    public AuthenticatorConfig() {}

    public AuthenticatorConfig(HashAlgorithm algo, int timeWindowSize, int windowRange, CodeLength codeLength) {
        setHashAlgorithm(algo);
        setWindowSize(timeWindowSize);
        allowedWindowOffset(windowRange);
        setNumCodeDigits(codeLength);
    }

    public AuthenticatorConfig setNumCodeDigits(CodeLength length) {
        this.codeLength = length;
        return this;
    }

    public AuthenticatorConfig allowedWindowOffset(int offset) {
        this.allowedOffset = offset;
        return this;
    }

    public AuthenticatorConfig setWindowSize(long secondsInTimeWindow) {
        this.secondsInTimeWindow = secondsInTimeWindow;
        return this;
    }

    public AuthenticatorConfig setHashAlgorithm(HashAlgorithm algo) {
        this.hashAlgorithm = algo;
        return this;
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public long getWindowSize() {
        return secondsInTimeWindow;
    }

    public int getNumCodeDigits() {
        return codeLength.getValue();
    }

    public int getWindowRange() {
        return allowedOffset;
    }
}
