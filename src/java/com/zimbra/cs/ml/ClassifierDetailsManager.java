package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 *
 * @author iraykin
 *
 */
public abstract class ClassifierDetailsManager {
	protected Mailbox mbox;

	public ClassifierDetailsManager(Mailbox mbox) {
		this.mbox = mbox;
	}
	public abstract String getReason(Integer itemId) throws ServiceException;

	public abstract void setReason(Integer itemId, String reason) throws ServiceException;

	public abstract void deleteReason(Integer itemId) throws ServiceException;
}
