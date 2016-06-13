/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.extensions;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.filter.JsieveConfigMapHandler;
import com.zimbra.cs.filter.jsieve.ActionTag;
import org.apache.jsieve.commands.AbstractActionCommand;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

/**
 * Dummy custom action extension for testing.
 *
 */
public class DummyCustomDiscard extends AbstractActionCommand implements ZimbraExtension {
    private static boolean initialized = false;
    private static boolean executed = false;
    private static boolean inactivated = false;

    public String getName() {
        return "discard";
    }

    public void init() {
        //if(!inactivated){
            ZimbraLog.extensions.info("init()");
            //JsieveConfigMapHandler.registerCommand("discard", this.getClass().getName());
            initialized = true;
        //}
    }

    public void destroy() {
        // after RuleManagerWithCustomActionFilterTest cases done, this class's class loader
        // is still active. So if some test execute ExtensionUtil.initAll, this class's init()
        // could be called again. So to be sure for JsieveConfigMapHandler.registerCommand() not to be called then,
        // have inactivated flag to be true here.
        inactivated = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isExecuted() {
        return executed;
    }

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        ZimbraLog.extensions.info("executeBasic()");
        mail.addAction(new ActionTag("priority"));
        executed = true;
        return null;
    }

}
