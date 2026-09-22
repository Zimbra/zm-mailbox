/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import java.util.Set;
import java.util.stream.Collectors;

public final class MailForwardingUtil {
    private MailForwardingUtil() {
    }

    public static void validateSelfForwarding(
            Account account,
            String forwardingAddresses)
            throws ServiceException {

        if (account == null || forwardingAddresses == null) {
            return;
        }

        Set<String> normalizedAccountAddresses =
                account.getAllAddrsSet()
                        .stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

        for (String addr : forwardingAddresses.split(",")) {

            String normalizedAddr = addr.trim().toLowerCase();

            if (!normalizedAddr.isEmpty()
                    && normalizedAccountAddresses.contains(normalizedAddr)) {

                ZimbraLog.account.warn(
                        "Attempt to set self-forwarding for account: %s, forwarding address: %s",
                        account.getName(),
                        addr);

                throw ServiceException.INVALID_REQUEST(
                        "Cannot set forwarding address to the same account",
                        null);
            }
        }
    }
}
