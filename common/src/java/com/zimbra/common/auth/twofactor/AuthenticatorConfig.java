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


public class AuthenticatorConfig {
    private TwoFactorOptions.HashAlgorithm hashAlgorithm;
    private TwoFactorOptions.CodeLength codeLength;
    private TwoFactorOptions.EmailCodeLength emailCodeLength;
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

    public AuthenticatorConfig setNumCodeDigits(TwoFactorOptions.EmailCodeLength emailCodeLength) {
        this.emailCodeLength = emailCodeLength;
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
        return codeLength != null ? codeLength.getValue() : emailCodeLength.getValue();
    }

    public int getWindowRange() {
        return allowedOffset;
    }
}
