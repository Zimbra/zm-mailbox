/*
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.mail.MessagingException;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.SyntaxException;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BodyTest extends AbstractTest {

    static final String CONTAINS = ":contains";
    
    /* (non-Javadoc)
     * @see org.apache.jsieve.tests.AbstractTest#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments)
     */
    protected boolean executeBasic(MailAdapter mail, Arguments arguments)
            throws SieveException {
        String comparator = null;
        String key = null;
        ListIterator argumentsIter = arguments.getArgumentList().listIterator();

        // First argument MUST be a tag of ":contains"
        // TODO: handles ":matches" with * and ?
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof TagArgument)
            {
                String tag = ((TagArgument) argument).getTag();
                if (tag.equals(CONTAINS))
                    comparator = tag;
                else
                    throw new SyntaxException(
                        "Found unexpected TagArgument: \"" + tag + "\"");
            }
        }
        if (null == comparator)
            throw new SyntaxException("Expecting \"" + CONTAINS + "\"");

        // Second argument MUST be a string
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                StringListArgument strList = (StringListArgument) argument;
                key = (String) strList.getList().get(0);
            }
        }
        if (null == key)
            throw new SyntaxException("Expecting a string");

        // There MUST NOT be any further arguments
        if (argumentsIter.hasNext())
            throw new SyntaxException("Found unexpected argument(s)");               
        if (!(mail instanceof ZimbraMailAdapter))
            return false;
        return test(mail, comparator, key);

    }
    
    protected void validateArguments(Arguments arguments) throws SieveException
    {
        // override validation -- it's already done in executeBasic above
    }

    private boolean test(MailAdapter mail, String comparator, String key) throws SieveException {
        ZimbraMailAdapter zimbraMail = (ZimbraMailAdapter) mail;
        ParsedMessage pm = zimbraMail.getParsedMessage();
        List msgParts = pm.getMessageParts();
        try {
            /*
             * We check the first level MIME parts that are text. If the key word appears there,
             * we consider it a match.
             */
            for (Iterator it = msgParts.iterator(); it.hasNext(); ) {
                MPartInfo part = (MPartInfo) it.next();
                Object content = part.getMimePart().getContent();
                if (content instanceof String) {
                    String contentStr = (String) content;
                    if (contentStr.toLowerCase().indexOf(key.toLowerCase()) >= 0) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
        } catch (MessagingException e) {
        }
        return false;
        
    }
}
