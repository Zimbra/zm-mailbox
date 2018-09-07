package com.zimbra.cs.index.solr;

import com.google.common.base.MoreObjects;
import com.zimbra.cs.index.ZimbraIndexDocumentID;

public class ZimbraSolrDocumentID implements Comparable<ZimbraSolrDocumentID>,
        ZimbraIndexDocumentID {
    private final String docID;
    private final String idField;

    public ZimbraSolrDocumentID(String idField, String docID) {
        this.idField = idField;
        this.docID = docID;
    }

    public String getDocID() {
        return docID;
    }

    public String getIDFIeld() {
        return idField;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("docID", docID).toString();
    }

    @Override
    public int compareTo(ZimbraSolrDocumentID o) {
        return docID.compareTo(o.getDocID());
    }

}
