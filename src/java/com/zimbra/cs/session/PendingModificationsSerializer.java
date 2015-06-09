package com.zimbra.cs.session;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public interface PendingModificationsSerializer {

    public String getContentEncoding();

    public String getContentType();

    public byte[] serialize(PendingModifications pendingMods) throws IOException;

    public PendingModifications deserialize(Mailbox mbox, byte[] data) throws IOException, ClassNotFoundException, ServiceException;
}