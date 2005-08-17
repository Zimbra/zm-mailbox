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
    protected boolean executeBasic(MailAdapter mail, Arguments arguments)
            throws SieveException {
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
