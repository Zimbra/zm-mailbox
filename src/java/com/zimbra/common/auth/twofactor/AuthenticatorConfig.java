package com.zimbra.common.auth.twofactor;


public class AuthenticatorConfig {
    private TwoFactorOptions.HashAlgorithm hashAlgorithm;
    private TwoFactorOptions.CodeLength codeLength;
    private long secondsInTimeWindow;
    private int allowedOffset;

    public AuthenticatorConfig() {}

    public AuthenticatorConfig(TwoFactorOptions.HashAlgorithm algo, int timeWindowSize, int windowRange, TwoFactorOptions.CodeLength codeLength) {
        setHashAlgorithm(algo);
        setWindowSize(timeWindowSize);
        allowedWindowOffset(windowRange);
        setNumCodeDigits(codeLength);
    }

    public AuthenticatorConfig setNumCodeDigits(TwoFactorOptions.CodeLength length) {
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

    public AuthenticatorConfig setHashAlgorithm(TwoFactorOptions.HashAlgorithm algo) {
        this.hashAlgorithm = algo;
        return this;
    }

    public TwoFactorOptions.HashAlgorithm getHashAlgorithm() {
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
