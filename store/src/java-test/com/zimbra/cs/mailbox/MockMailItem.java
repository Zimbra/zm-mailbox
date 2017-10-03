/**
 *
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;

/**
 * @author gsolovyev
 *
 */
public class MockMailItem extends MailItem {

    /**
     * @param mbox
     * @param data
     * @throws ServiceException
     */
    public MockMailItem(Mailbox mbox, UnderlyingData data)
            throws ServiceException {
        super(mbox, data, true);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param mbox
     * @param data
     * @param skipCache
     * @throws ServiceException
     */
    public MockMailItem(Mailbox mbox, UnderlyingData data, boolean skipCache)
            throws ServiceException {
        super(mbox, data, skipCache);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#getSender()
     */
    @Override
    public String getSender() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#isTaggable()
     */
    @Override
    boolean isTaggable() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#isCopyable()
     */
    @Override
    boolean isCopyable() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#isMovable()
     */
    @Override
    boolean isMovable() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#isMutable()
     */
    @Override
    boolean isMutable() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#canHaveChildren()
     */
    @Override
    boolean canHaveChildren() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailbox.MailItem#encodeMetadata(com.zimbra.cs.mailbox.Metadata)
     */
    @Override
    Metadata encodeMetadata(Metadata meta) {
        // TODO Auto-generated method stub
        return null;
    }

}
