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
package com.zimbra.client;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.account.message.TwoFactorCredentials;

public class ZTOTPCredentials {
    private String secret;
    private Set<String> scratchCodes;

    public ZTOTPCredentials(TwoFactorCredentials twoFactorCredentials) throws ServiceException {
        secret = twoFactorCredentials.getSharedSecret();
        scratchCodes = new HashSet<String>();
        for (String code: twoFactorCredentials.getScratchCodes()) {
            scratchCodes.add(code);
        }
    }

    public String getSecret() {
        return secret;
    }

    public Set<String> getScratchCodes() {
        return scratchCodes;
    }
}
