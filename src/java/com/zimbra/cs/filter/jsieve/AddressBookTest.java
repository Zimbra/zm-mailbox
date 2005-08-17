/*
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.SyntaxException;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.index.LiquidQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AddressBookTest extends AbstractTest {

    static final String IN = ":in";
    static final String CONTACTS = "contacts";
    static final String GAL = "GAL";
    static final byte[] SEARCH_TYPE = { MailItem.TYPE_CONTACT };
    private static Log mLog = LogFactory.getLog(AddressBookTest.class);
    
    /* (non-Javadoc)
     * @see org.apache.jsieve.tests.AbstractTest#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments)
     */
    protected boolean executeBasic(MailAdapter mail, Arguments arguments)
            throws SieveException {
        String comparator = null;
        Set abooks = null;
        ListIterator argumentsIter = arguments.getArgumentList().listIterator();

        // First argument MUST be a tag of ":in"
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof TagArgument)
            {
                String tag = ((TagArgument) argument).getTag();
                if (tag.equals(IN))
                    comparator = tag;
                else
                    throw new SyntaxException(
                        "Found unexpected: \"" + tag + "\"");
            }
        }
        if (null == comparator)
            throw new SyntaxException("Expecting \":in\"");

        // Second argument MUST be header names
        String[] headers = null;
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                StringListArgument strList = (StringListArgument) argument;
                headers = new String[strList.getList().size()];
                for (int i=0; i< headers.length; i++) {
                    headers[i] = (String) strList.getList().get(i);
                }
            }
        }
        if (headers == null) {
            throw new SyntaxException("No headers are found");
        }
        // Third argument MUST be either contacts or GAL
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                StringListArgument strList = (StringListArgument) argument;
                abooks = new HashSet(3);
                for (int i=0; i< strList.getList().size(); i++) {
                    String abookName = (String) strList.getList().get(i);
                    if (!CONTACTS.equals(abookName) && !GAL.equals(abookName))
                        throw new SyntaxException("Unknown address book name: " + abookName);
                    // eliminate duplicates by adding it to the set
                    abooks.add(abookName);
                }
            }
        }
        if (abooks == null || abooks.isEmpty())
            throw new SyntaxException("Expecting address book name(s)");

        // There MUST NOT be any further arguments
        if (argumentsIter.hasNext())
            throw new SyntaxException("Found unexpected argument(s)");               
        
        if (! (mail instanceof ZimbraMailAdapter))
            return false;
        return test(mail, comparator, headers, abooks);
    }

    private boolean test(MailAdapter mail, String comparator, String[] headers, Set abooks) throws SieveException {
        ZimbraMailAdapter liquidMail = (ZimbraMailAdapter) mail;
        for (Iterator it = abooks.iterator(); it.hasNext(); ) {
            String abookName = (String) it.next();
            if (CONTACTS.equals(abookName)) {
                Mailbox mbox = liquidMail.getMailbox();
                // searching contacts
                for (int i=0; i<headers.length; i++) {
                    // get values for header that should contains address, like From, To, etc.
                    List headerVals = mail.getHeader(headers[i]);
                    for (int k=0; k<headerVals.size(); k++) {
                        // each header may contain multiple vaules; e.g., To: may contain many recipients
                        String headerVal = ((String) headerVals.get(k)).toLowerCase();
                        LiquidQueryResults results = null;
                        try {
                            String iaddrStr = headerVal;
                            try {
                                InternetAddress iaddr = new InternetAddress(headerVal);
                                iaddrStr = iaddr.getAddress();
                            } catch (AddressException e1) {
                            }
                            results = mbox.search("To:" + iaddrStr, 
                                    SEARCH_TYPE, MailboxIndex.SEARCH_ORDER_DATE_ASC);
                            mLog.debug("searching for " + iaddrStr);
                            if (results.hasNext()) {
                                mLog.debug("found " + iaddrStr + " in contacts");
                                return true;
                            }
                        } catch (IOException e) {
                        } catch (ParseException e) {
                        } catch (ServiceException e) {
                        } finally {
                            if (results != null) {
                                try {
                                    results.doneWithSearchResults();
                                } catch (ServiceException e) {
                                }
                            }
                        }
                    }
                }
                
            } // searching other address database like GAL
        }
        return false;
    }
    
    protected void validateArguments(Arguments arguments) throws SieveException
    {
    }
}
