package com.zimbra.cs.imap;

import java.util.List;

import com.zimbra.cs.imap.ImapSession.ImapFlag;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.ServiceException;

public interface IFindOrCreateTags {
	public List<ImapFlag> doFindOrCreateTags(List<String> flagNames, List<Tag> newTags) throws ServiceException;
}
