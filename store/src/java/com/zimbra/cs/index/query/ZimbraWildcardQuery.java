package com.zimbra.cs.index.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.parser.SolrQueryParserBase.MagicFieldName;

import com.google.common.base.Joiner;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.index.solr.SolrIndex.LocalParams;
import com.zimbra.cs.index.solr.SolrUtils;

public class ZimbraWildcardQuery extends org.apache.lucene.search.Query {

    private String queryString;
    private List<String> fields;
    private boolean expandAll;
    private int maxNgramSize;

    public ZimbraWildcardQuery(String queryString, String...fields) {
        this.queryString = queryString;
        this.fields = new ArrayList<>();
        addFields(fields);
    }

    public ZimbraWildcardQuery addFields(String... fields) {
        if (fields != null) {
            for (String field: fields) {
                this.fields.add(field);
            }
        }
        return this;
    }

    public void setExpandAll(boolean expandAll) {
        this.expandAll = expandAll;
    }

    public void setMaxNgramSize(int maxNgramSize) {
        this.maxNgramSize = maxNgramSize;
    }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) &&
                fields.equals(((ZimbraWildcardQuery) other).fields) &&
                queryString.equals(((ZimbraWildcardQuery) other).queryString);
    }

    @Override
    public int hashCode() {
        return fields.hashCode() ^ queryString.hashCode();
    }

    @Override
    public String toString(String arg0) {
        LocalParams lp = new LocalParams("zimbrawildcard");
        lp.addParam("fields", Joiner.on(" ").join(fields));
        lp.addParam("v", queryString);
        lp.addParam("maxExpansions", String.valueOf(LC.zimbra_index_wildcard_max_terms_expanded.intValue()));
        if (expandAll) {
            lp.addParam("expandAll", "true");
        }
        if (maxNgramSize > 0) {
            lp.addParam("maxNgramSize", String.valueOf(maxNgramSize));
        }
        return String.format("%s:\"%s\"", MagicFieldName.QUERY, SolrUtils.escapeQuotes(lp.encode()));
    }

}
