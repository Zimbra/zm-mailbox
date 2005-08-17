/*
 * Created on Apr 11, 2005
 *
 */
package com.liquidsys.coco.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.jsieve.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.SieveMailException;

/**
 *  Dummy mail adapter for evaluating sieve rules when they are saved.
 *  This catches any syntax errors early on.
 *  @author kchen
 */
public class DummyMailAdapter implements MailAdapter {

    private List mHeaders = new ArrayList(1);
    private List mActions = new ArrayList(1);
    
    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#getActions()
     */
    public List getActions() {
        return mActions;
    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#getActionsIterator()
     */
    public ListIterator getActionsIterator() {
        return mActions.listIterator();
    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#getHeader(java.lang.String)
     */
    public List getHeader(String name) throws SieveMailException {
        return Collections.EMPTY_LIST;
    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#getMatchingHeader(java.lang.String)
     */
    public List getMatchingHeader(String name) throws SieveMailException {
        return mHeaders;
    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#getHeaderNames()
     */
    public List getHeaderNames() throws SieveMailException {
        return Collections.EMPTY_LIST;
    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#addAction(org.apache.jsieve.mail.Action)
     */
    public void addAction(Action action) {

    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#executeActions()
     */
    public void executeActions() throws SieveException {
    }

    /* (non-Javadoc)
     * @see org.apache.jsieve.mail.MailAdapter#getSize()
     */
    public int getSize() throws SieveMailException {
        return 0;
    }

}
