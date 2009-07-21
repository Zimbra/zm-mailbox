package com.zimbra.cs.index;

public class IndexDocument {
    public IndexDocument(Object wrapped) { mWrappedDocument = wrapped; }
    public Object getWrappedDocument() { return mWrappedDocument; } ;
    
    private Object mWrappedDocument;
}
