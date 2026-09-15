/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GenerateSecretKeyCallbackTest {

    @Test
    public void preModifyDomainCreateWithFalseIsNotDenied() throws Exception {
        GenerateSecretKeyCallback callback = new GenerateSecretKeyCallback();
        CallbackContext context = new CallbackContext(CallbackContext.Op.CREATE);
        context.setCreatingEntryType(Domain.class);

        callback.preModify(context, Provisioning.A_zimbraFeatureMailRecallEnabled,
                ProvisioningConstants.FALSE, new HashMap<String, Object>(), null);
    }

    @Test
    public void preModifyDomainCreateWithTrueIsDenied() {
        GenerateSecretKeyCallback callback = new GenerateSecretKeyCallback();
        CallbackContext context = new CallbackContext(CallbackContext.Op.CREATE);
        context.setCreatingEntryType(Domain.class);

        try {
            callback.preModify(context, Provisioning.A_zimbraFeatureMailRecallEnabled,
                    ProvisioningConstants.TRUE, new HashMap<String, Object>(), null);
            fail("expected create-time domain enablement to be rejected");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("cannot be configured at the domain or global config level"));
        }
    }
}

