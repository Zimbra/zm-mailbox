package com.zimbra.cs.index;

import java.io.IOException;

import com.zimbra.cs.index.IndexWritersCache.IndexWriter;

interface IIndexWritersCache {

    public abstract void flushAllWriters();

    public abstract void beginWriting(IndexWriter w) throws IOException;

    public abstract void doneWriting(IndexWriter w);

    public abstract void shutdown();
    
    public abstract void flush(IndexWriter target);
    
}