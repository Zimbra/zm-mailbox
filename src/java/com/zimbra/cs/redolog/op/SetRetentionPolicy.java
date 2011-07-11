/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.RetentionPolicy;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetRetentionPolicy extends RedoableOp {

    private int itemId = -1;
    private List<RetentionPolicy> keepPolicy = Lists.newArrayList();
    private List<RetentionPolicy> purgePolicy = Lists.newArrayList();

    public SetRetentionPolicy() {
        super(MailboxOperation.SetRetentionPolicy);
    }

    public SetRetentionPolicy(int mailboxId, int itemId, Iterable<RetentionPolicy> keepPolicy, Iterable<RetentionPolicy> purgePolicy) {
        this();
        setMailboxId(mailboxId);
        this.itemId = itemId;
        if (keepPolicy != null) {
            Iterables.addAll(this.keepPolicy, keepPolicy);
        }
        if (purgePolicy != null) {
            Iterables.addAll(this.purgePolicy, purgePolicy);
        }
    }
    
    @Override protected String getPrintableData() {
        return "keepPolicy=" + keepPolicy + ", deletePolicy=" + purgePolicy;
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(itemId);
        out.writeInt(keepPolicy.size());
        for (RetentionPolicy policy : keepPolicy) {
            out.writeUTF(policy.getId());
            out.writeUTF(policy.getLifetimeString());
        }
        out.writeInt(purgePolicy.size());
        for (RetentionPolicy policy : purgePolicy) {
            out.writeUTF(policy.getId());
            out.writeUTF(policy.getLifetimeString());
        }
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        itemId = in.readInt();
        int size = in.readInt();
        keepPolicy = readPolicyList(in, size);
        size = in.readInt();
        purgePolicy = readPolicyList(in, size);
    }

    /**
     * Reads a list of {@code RetentionPolicy} objects from input.
     */
    private List<RetentionPolicy> readPolicyList(RedoLogInput in, int size)
    throws IOException {
        List<RetentionPolicy> list = Lists.newArrayList();
        for (int i = 1; i <= size; i++) {
            String id = in.readUTF();
            String duration = in.readUTF();
            RetentionPolicy p;
            if (id != null) {
                p = RetentionPolicy.newSystemPolicy(id);
            } else {
                try {
                    p = RetentionPolicy.newUserPolicy(duration);
                } catch (ServiceException e) {
                    throw new IOException(e);
                }
            }
            list.add(p);
        }
        return list;
    }

    
    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        // TODO: Support tags
        mbox.setRetentionPolicy(getOperationContext(), itemId, MailItem.Type.FOLDER, keepPolicy, purgePolicy);
    }

    //////////////// Unit test methods /////////////////
    
    int getItemId() {
        return itemId;
    }
    
    List<RetentionPolicy> getKeepPolicy() {
        return keepPolicy;
    }
    
    List<RetentionPolicy> getPurgePolicy() {
        return purgePolicy;
    }
    
    byte[] testSerialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializeData(new RedoLogOutput(out));
        return out.toByteArray();
    }
    
    void testDeserialize(byte[] data) throws IOException {
        deserializeData(new RedoLogInput(new ByteArrayInputStream(data)));
    }
}
