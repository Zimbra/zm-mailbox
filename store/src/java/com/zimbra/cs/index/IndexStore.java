/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.PrintStream;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.Zimbra;

/**
 * Abstraction of index store backend.
 *
 * @author ysasaki
 */
public abstract class IndexStore {

    private static Factory factory;

    /**
     * {@link Indexer#close()} must be called after use.
     */
    public abstract Indexer openIndexer() throws IOException;

    /**
     * {@link ZimbraIndexSearcher#close()} must be called after use.
     */
    public abstract ZimbraIndexSearcher openSearcher() throws IOException;

    /**
     * Prime the index.
     */
    public abstract void warmup();

    /**
     * Removes any IndexSearcher used for this index from cache - if appropriate
     */
    public abstract void evict();

    /**
     * Deletes the whole index data for the mailbox.
     */
    public abstract void deleteIndex() throws IOException;

    /**
     * Get value of Flag that indicates that the index is scheduled for deletion
     */
    public abstract boolean isPendingDelete();

    /**
     * Set Flag to indicate that the index is scheduled for deletion
     */
    public abstract void setPendingDelete(boolean pendingDelete);

    /**
     * Primes the index for the fastest available search if the underlying IndexStore supports (and benefits from)
     * an appropriate optimization feature.
     */
    public abstract void optimize();

    /**
     * Runs a sanity check for the index data.  Used by the "VerifyIndexRequest" SOAP Admin request
     */
    public abstract boolean verify(PrintStream out) throws IOException;

    /**
     * Get the status that the index data (files, etc.) is created or not
     */
    public abstract boolean isIndexExist() throws IOException;

    public static Factory getFactory() {
        if (factory == null) {
            setFactory(LC.zimbra_class_index_store_factory.value());
        }
        return factory;
    }

    public static void setFields(MailItem item, IndexDocument doc) {
        doc.removeSortSubject();
        doc.addSortSubject(item.getSortSubject());
        doc.removeSortName();
        doc.addSortName(item.getSortSender());
        doc.removeMailboxBlobId();
        doc.addMailboxBlobId(item.getId());
        // If this doc is shared by multi threads, then the date might just be wrong,
        // so remove and re-add the date here to make sure the right one gets written!
        doc.removeSortDate();
        doc.addSortDate(item.getDate());
        doc.removeSortSize();
        doc.addSortSize(item.getSize());
        doc.removeSortAttachment();
        doc.addSortAttachment(item.hasAttachment());
        doc.removeSortFlag();
        doc.addSortFlag(item.isFlagged());
        doc.removeSortPriority();
        doc.addSortPriority(item.getFlagBitmask());
}

@VisibleForTesting
    public static final void setFactory(String factoryClassName) {
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = ExtensionUtil.findClass(factoryClassName)
                            .asSubclass(Factory.class);
                } catch (ClassNotFoundException cnfe) {
                    Zimbra.halt("Unable to initialize Index Store for class " + factoryClassName, cnfe);
                }
            }
        } catch (ClassCastException cce) {
            Zimbra.halt("Unable to initialize Index Store for class " + factoryClassName, cce);
        }
        setFactory(factoryClass);
        ZimbraLog.index.info("Using Index Store %s", factory.getClass().getDeclaringClass().getSimpleName());
    }

    private static synchronized final void setFactory(Class<? extends Factory> factoryClass) {
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException ie) {
            Zimbra.halt("Unable to initialize Index Store for " + factoryClass.getDeclaringClass().getSimpleName(), ie);
        } catch (IllegalAccessException iae) {
            Zimbra.halt("Unable to initialize Index Store for " + factoryClass.getDeclaringClass().getSimpleName(), iae);
        }
    }

    public interface Factory {
        /**
         * Get an IndexStore instance for a particular mailbox
         */
        IndexStore getIndexStore(Mailbox mbox) throws ServiceException;

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        void destroy();
    }
}
