/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.purge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Threader;
import com.zimbra.cs.mime.ParsedMessage;

public abstract class DataSourcePurge {

    protected Mailbox mbox;
    private static long PURGE_BATCH_SIZE = 1000000L;
    protected Map<String, ConversationPurgeQueue> purgeQueues = new HashMap<String,ConversationPurgeQueue>();

    public DataSourcePurge(Mailbox mbox) {
        this.mbox = mbox;
    }

    protected List<DataSource> getAllDataSources() throws ServiceException {
        return mbox.getAccount().getAllDataSources();
    }

    abstract List<DataSource> getPurgeableDataSources(DataSource incoming) throws ServiceException;

    public void purgeConversations(OperationContext octxt, DataSource incoming, long bytesToPurge, Integer convId) throws ServiceException {
        long bytesLeft = bytesToPurge;
        while (bytesLeft > 0L) {
            long toPurge = Math.min(bytesLeft, PURGE_BATCH_SIZE);
            PurgeableConvs convsToPurge = getOldestConversations(getPurgeableDataSources(incoming), toPurge, convId);
            bytesLeft -= convsToPurge.getTotalSize();
            if (convsToPurge != null && convsToPurge.getTotalSize() > 0) {
                long bytesLeftAfterThis = Math.max(0, bytesLeft - convsToPurge.getTotalSize());
                ZimbraLog.datasource.info("purging %d conversations to free up %d bytes; %d bytes left", convsToPurge.getNumConvs(), convsToPurge.getTotalSize(), bytesLeftAfterThis);
                for (PurgeableConv conv: convsToPurge.getConvs()) {
                    purgeConversation(octxt, conv);
                }
            }
        }
    }

    private void purgeConversation(OperationContext octxt, PurgeableConv conv) throws ServiceException {
        if (conv.isMsg()) {
            Message message = mbox.getMessageById(null, conv.getId());
            ZimbraLog.datasource.info(String.format("purging message %d", conv.getId()));
            purgeMailItem(octxt, conv.getDataSourceId(), message, Type.MESSAGE);
        } else {
            Conversation conversation = mbox.getConversationById(null, conv.getId());
            ZimbraLog.datasource.info(String.format("purging conversation %d", conv.getId()));
            purgeMailItem(octxt, conv.getDataSourceId(), conversation, Type.CONVERSATION);
        }
    }

    private void partiallyPurgeConversation(OperationContext octxt, String dataSourceId, Conversation conversation, Set<Integer> ids) throws ServiceException {
        Integer convId = conversation.getId();
        for (Message msg: mbox.getMessagesByConversation(null, conversation.getId())) {
            if (ids.contains(msg.getId())) {
                ParsedMessage pm = msg.getParsedMessage();
                Threader threader = pm.getThreader(mbox);
                threader.storePurgedConversationHashes(convId, dataSourceId);
                mbox.purgeDataSourceMessage(octxt, msg, dataSourceId);
                mbox.delete(null, msg.getId(), MailItem.Type.MESSAGE);
            }
        }
    }

    private void purgeMailItem(OperationContext octxt, String dataSourceId, MailItem item, MailItem.Type type) throws ServiceException {
        if (type == Type.MESSAGE) {
            mbox.purgeDataSourceMessage(octxt, (Message) item, dataSourceId);
        } else if (type == Type.CONVERSATION) {
            Conversation conv = (Conversation) item;
            Set<Integer> msgIdsInThisDataSource = DbDataSource.getConvMessageIdsInDataSource(mbox, conv.getId(), dataSourceId);
            if (conv.getMessageCount() > msgIdsInThisDataSource.size()) {
                partiallyPurgeConversation(octxt, dataSourceId, conv, msgIdsInThisDataSource);
                return;
            } else {
                for (Message msg: mbox.getMessagesByConversation(null, conv.getId())) {
                    mbox.purgeDataSourceMessage(octxt, msg, dataSourceId);
                }
            }
        } else {
            throw ServiceException.FAILURE("can only purge messages and conversations", null);
        }
        DbDataSource.moveToPurgedConversations(mbox, item, dataSourceId);
        mbox.delete(null, item.getId(), type);
    }

    private String dataSourcePurgeQueueKey(List<DataSource> dataSources) {
        String accountId = mbox.getAccountId();
        if (dataSources.size() == 1) {
            return String.format("%s:%s", accountId, dataSources.get(0).getId());
        }
        List<String> dsIds = new ArrayList<String>();
        for (DataSource ds: dataSources) {
            dsIds.add(ds.getId());
        }
        Collections.sort(dsIds);
        return String.format("%s:%s", accountId, Joiner.on(",").join(dsIds));
    }

    protected PurgeableConvs getOldestConversations(List<DataSource> dataSources, long size, Integer convId) throws ServiceException {
        ZimbraLog.datasource.info(String.format("finding %d bytes to purge from %d data sources", size, dataSources.size()));
        if (convId != null && convId < 0) {
          /* The incoming message is the second message in a thread.
             Flip the ID so that we know when we come across the message and don't purge it */
            convId = -1 * convId;
        }
        long accumulated = 0L;
        PurgeableConvs convs = new PurgeableConvs();
        ConversationPurgeQueue purgeQueue;
        String cacheKey = dataSourcePurgeQueueKey(dataSources);
        if (purgeQueues.containsKey(cacheKey)) {
            purgeQueue = purgeQueues.get(cacheKey);
        } else {
            purgeQueue = new ConversationPurgeQueue();
            purgeQueues.put(cacheKey, purgeQueue);
        }
        long startDate = 0;
        while (accumulated <= size) {
            if (purgeQueue.isEmpty()) {
                //buffer oldest conversations from DB
                long convSizeToBuffer = Provisioning.getInstance().getConfig().getPurgedConversationsQueueSize();
                boolean hasData = false;
                for (PurgeableConv conv: DbDataSource.getOldestConversationsUpToSize(dataSources, convSizeToBuffer, startDate)) {
                    hasData = true;
                    purgeQueue.enqueue(conv);
                }
                if (!hasData) {
                    //edge case: we ran out of purgeable conversations.
                    //this can really only happen if the only conversation left is the one that's being appended to,
                    //or if the incoming message is larger than the data source quota
                    ZimbraLog.datasource.warn(String.format("cannot purge sufficient data for data source %s (%s)", cacheKey, cacheKey));
                    return convs;
                }
            }
            PurgeableConv conv = purgeQueue.dequeue();
            startDate = conv.getDate();
            if (convId != null && convId == conv.getId()) {
                // Don't want to purge a conversation that the incoming message is part of.
                // TODO: what happens if this is the second message in the conversation?
                continue;
            }
            accumulated += conv.getSize();
            convs.add(conv);
        }
        return convs;
    }

    public static class PurgeableConv {
        private int id;
        private int numMsgs;
        private long size;
        private long date;
        private String dataSourceId;

        public PurgeableConv(int id, long size, long date, String datasSourceId, int numMsgs) {
            this.id = id;
            this.size = size;
            this.date = date;
            this.dataSourceId = datasSourceId;
            this.numMsgs = numMsgs;
        }

        public boolean isMsg() {
            return numMsgs == 1;
        }

        public int getId() {
            return id;
        }

        public long getSize() {
            return size;
        }

        public long getDate() {
            return date;
        }

        public String getDataSourceId() {
            return dataSourceId;
        }

        public int getNumMessages() {
            return numMsgs;
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
            helper.add("id", id);
            helper.add("size", size);
            helper.add("date", date);
            helper.add("data source", dataSourceId);
            helper.add("# messages", numMsgs);
            return helper.toString();
        }
    }

    private static class PurgeableConvs {
        private long totalSize;
        private long latestDate;
        private List<PurgeableConv> convs;

        private PurgeableConvs() {
            totalSize = 0L;
            latestDate = 0L;
            convs = new LinkedList<PurgeableConv>();
        }

        void add(PurgeableConv conv) {
            totalSize += conv.getSize();
            if (conv.getDate() > latestDate) {
                latestDate = conv.getDate();
            }
            convs.add(conv);
        }

        long getTotalSize() {
            return totalSize;
        }

        int getNumConvs() {
            return convs.size();
        }

        long getLatestDate() {
            return latestDate;
        }

        List<PurgeableConv> getConvs() {
            return convs;
        }
    }

    /* This is a queue implemented as a doubly-linked list, with the additional property
     * that when a node instance is dequeued from one queue, any nodes that contain the same
     * OldestConv id are deleted from the queue as well. This is necessary because
     * multiple purge queues could potentially share the same items, and we don't want to purge
     * a conversation twice.
     */
    public static class ConversationPurgeQueue {
        // map that is used to look up all nodes in all queues containing OldestConvs with the same ID as the key
        private static Map<Integer, LinkedList<Node>> nodes = new HashMap<Integer, LinkedList<Node>>();
        static class Node {
            private PurgeableConv conv;
            private Node next = null;
            private Node prev = null;
            private ConversationPurgeQueue queue;
            public Node(ConversationPurgeQueue queue, PurgeableConv conv) {
                this.conv = conv;
                this.queue = queue;
            }
            public PurgeableConv getConv() { return conv; }
            public void setNext(Node node) { this.next = node; }
            public void setPrev(Node node) { this.prev = node; }
            public Node getNext() { return next; }
            private void remove() {
                queue.length--;
                if (prev == null && next == null) {
                    queue.head = queue.tail = null;
                    return;
                }
                if (next == null) {
                    //removing last node
                    queue.tail = prev;
                    queue.tail.next = null;
                } else if (prev == null) {
                    //removing first node
                    queue.head = next;
                    queue.head.prev = null;
                } else {
                    prev.next = next;
                    next.prev = prev;
                }
                prev = next = null;
            }
        }

        private Map<Integer, Node> map = new HashMap<Integer, Node>();
        private Node head;
        private Node tail = new Node(this, null);
        private int length;

        public ConversationPurgeQueue() {
            head = tail = null;
            length = 0;
        }

        public void enqueue(PurgeableConv conv) {
            Node node = new Node(this, conv);
            if (head == null) {
                head = node;
            } else {
                tail.setNext(node);
                node.setPrev(tail);
            }
            tail = node;
            map.put(conv.getId(), node);
            LinkedList<Node> instances = nodes.get(conv.getId());
            if (instances == null) {
                instances = new LinkedList<Node>();
                nodes.put(conv.getId(), instances);
            }
            instances.add(node);
            length++;
        }

        public PurgeableConv dequeue() {
            if (head == null) {
                return null;
            }
            length--;
            Node toReturn = head;
            head = head.getNext();
            PurgeableConv conv = toReturn.getConv();
            List<Node> allSuchNodes = nodes.get(conv.getId());
            for (Node node: allSuchNodes) {
                if (node != toReturn) {
                    node.remove();
                }
            }
            nodes.remove(conv.getId());
            return conv;
        }

        public int size() {
            return length;
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public static void removeAllNodesById(Integer id) {
            List<Node> nodesWithId = nodes.get(id);
            if (nodesWithId != null) {
                for (Node node: nodesWithId) {
                    node.remove();
                }
            }
        }
    }
}
