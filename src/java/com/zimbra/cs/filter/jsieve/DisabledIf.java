/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 11, 2005
 *
 */
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.commands.AbstractConditionalCommand;
import org.apache.jsieve.mail.MailAdapter;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DisabledIf extends AbstractConditionalCommand {

    /* (non-Javadoc)
     * @see org.apache.jsieve.commands.AbstractCommand#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments, org.apache.jsieve.Block)
     */
    protected Object executeBasic(MailAdapter mail, Arguments arguments,
            Block block) throws SieveException {
        return null;
    }
    
    protected void validateArguments(Arguments arguments) throws SieveException
    {
    }
}
