/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AttachmentTest extends AbstractTest {

    /* (non-Javadoc)
     * @see org.apache.jsieve.tests.AbstractTest#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments)
     */
    protected boolean executeBasic(MailAdapter mail, Arguments arguments) {
        if (!(mail instanceof ZimbraMailAdapter))
            return false;
        // arguments already validated by superclass's validateArguments
        return ((ZimbraMailAdapter) mail).getParsedMessage().hasAttachments();
    }

    protected void validateArguments(Arguments arguments) throws SieveException
    {
        super.validateArguments(arguments);
    }
}
