package com.zimbra.cs.index;

import java.util.List;

import org.apache.lucene.document.Field;

public class IndexDocument {
    public IndexDocument(Object wrapped) { mWrappedDocument = wrapped; }
    public Object getWrappedDocument() { return mWrappedDocument; } ;
    
    private Object mWrappedDocument;
    
    
    public void dump(StringBuilder sb) {
        Object wd = getWrappedDocument();
        if (wd instanceof org.apache.lucene.document.Document) {
            org.apache.lucene.document.Document luceneDoc = (org.apache.lucene.document.Document)wd;
            sb.append("Lucene doc \n");
                
            for (Field field : (List<Field>)luceneDoc.getFields()) {
                String name = field.name();
                String value = field.stringValue();
                sb.append("    Field " + name + ": " + value + "\n");
            }
        }
    }
}
