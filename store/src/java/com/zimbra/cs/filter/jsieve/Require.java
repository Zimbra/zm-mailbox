/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import static com.zimbra.cs.filter.JsieveConfigMapHandler.CAPABILITY_EDITHEADER;
import static org.apache.jsieve.Constants.COMPARATOR_PREFIX;
import static org.apache.jsieve.Constants.COMPARATOR_PREFIX_LENGTH;

import java.util.List;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * Class Require implements the Require control as defined in RFC 5228, section 3.2.
 */
public class Require extends org.apache.jsieve.commands.Require {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;

        final List<String> stringArgumentList = ((StringListArgument) arguments
                .getArgumentList().get(0)).getList();
        for (String stringArgument: stringArgumentList) {
            validateFeature(stringArgument, mail, context);
            if (CAPABILITY_EDITHEADER.equals(getCapabilityString(stringArgument))
                && mailAdapter.isUserScriptExecuting()) {
                throw new SieveException(RuleManager.editHeaderUserScriptError);
            }
            mailAdapter.addCapabilities(getCapabilityString(stringArgument));
        }
        return null;
    }

    private String getCapabilityString(String name) {
        if (name.startsWith(COMPARATOR_PREFIX)) {
            return name.substring(COMPARATOR_PREFIX_LENGTH);
        } else {
            return name;
        }
    }

    public static void checkCapability(MailAdapter mail, String capability) throws SyntaxException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return;
        }
        ZimbraMailAdapter zma  = (ZimbraMailAdapter) mail;
        if (!Require.isSieveRequireControlRFCCompliant(zma)) {
            return;
        }

        if (!zma.isCapable(capability)) {
            throw new SyntaxException("Undeclared extension (" + capability + ")");
        }
    }

    public static boolean isSieveRequireControlRFCCompliant(MailAdapter mail) {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return true;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        return mailAdapter.getAccount().isSieveRequireControlEnabled();
    }
}
