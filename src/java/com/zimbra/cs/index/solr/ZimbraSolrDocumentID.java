package com.zimbra.cs.index.solr;

import com.google.common.base.Objects;
import com.zimbra.cs.index.ZimbraIndexDocumentID;

public class ZimbraSolrDocumentID implements Comparable<ZimbraSolrDocumentID>,
        ZimbraIndexDocumentID {
    private final String docID;

    public ZimbraSolrDocumentID(String docID) {
        this.docID = docID;
    }

    public String getDocID() {
        return docID;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("docID", docID).toString();
    }

    @Override
    public int compareTo(ZimbraSolrDocumentID o) {
        return docID.compareTo(o.getDocID());
    }

}
