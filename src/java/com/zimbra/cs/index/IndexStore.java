/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.lucene.search.IndexSearcher;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;
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
     * Removes from cache.
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
     * Runs a sanity check for the index data.
     */
    public abstract boolean verify(PrintStream out) throws IOException;

    public static Factory getFactory() {
        if (factory == null) {
            setFactory(LC.zimbra_class_index_store_factory.value());
        }
        return factory;
    }

    @VisibleForTesting
    public static final void setFactory(String factoryClassName) {
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                factoryClass = (Class<? extends Factory>) Class.forName(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = (Class<? extends Factory>)ExtensionUtil.findClass(factoryClassName)
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
