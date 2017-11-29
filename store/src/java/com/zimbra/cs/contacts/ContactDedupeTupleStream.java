package com.zimbra.cs.contacts;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.Explanation;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.expr.Expressible;
import org.apache.solr.client.solrj.io.stream.expr.StreamExplanation;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mime.ParsedAddress;

/**
 * Custom stream decorator used to deduplicate related contacts.
 * In order to do this, we consume the entire stream and merge tuples representing
 * the same contact, adding up their counts. The emitted tuple contains the longer of the
 * address values.
 *
 * Optionally, a list of filter values can be provided; contacts that match
 * will be skipped.
 *
 * For example, tuples for "bob smith <bsmith@zimbra.com>" and "bsmith@zimbra.com"
 * would be merged into the longer of the two addresses.
 *
 * @author iraykin
 *
 */
public class ContactDedupeTupleStream extends TupleStream implements Expressible {

    private static final long serialVersionUID = 1;

    private TupleStream stream;
    private Multimap<String, Tuple> normalizedMap;
    private LinkedList<Tuple> merged;
    private Tuple EOF;
    private String addrField;
    private String countField;
    private Set<String> filterValues;


    public ContactDedupeTupleStream(TupleStream stream, String addrField, String countField, Collection<String> filterValues) {
        this.stream = stream;
        normalizedMap = ArrayListMultimap.create();
        merged = Lists.newLinkedList();
        this.addrField = addrField;
        this.countField = countField;
        buildFilter(filterValues);
    }

    public ContactDedupeTupleStream(TupleStream stream, String addrField, String countField) {
        this(stream, addrField, countField, null);
    }

    private void buildFilter(Collection<String> filterAddrs) {
        if (filterAddrs == null || filterAddrs.isEmpty()) {
            filterValues = Sets.newHashSet();
        }else {
            filterValues = filterAddrs.stream().map(addr -> normalizeEmail(addr)).collect(Collectors.toSet());
        }
    }
    @Override
    public StreamExpression toExpression(StreamFactory factory) throws IOException{
      return toExpression(factory, true);
    }

    private StreamExpression toExpression(StreamFactory factory, boolean includeStreams) throws IOException {
      StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));

      if(includeStreams){
        if(stream instanceof Expressible){
          expression.addParameter(((Expressible)stream).toExpression(factory));
        }
        else{
          throw new IOException("ContactDedupeTupleStream contains a non-expressible TupleStream - it cannot be converted to an expression");
        }
      }
      else{
        expression.addParameter("<stream>");
      }
      return expression;
    }

    @Override
    public void setStreamContext(StreamContext context) {
        stream.setStreamContext(context);
    }

    @Override
    public List<TupleStream> children() {
        return Lists.newArrayList(stream);
    }

    private String getEmailAddr(Tuple tuple) {
        return tuple.getString(addrField);
    }

    private long getCount(Tuple tuple) {
        return tuple.getLong(countField);
    }

    private String normalizeEmail(String address) {
        ParsedAddress parsed = new ParsedAddress(address);
        String normalized = parsed.emailPart;
        return Strings.isNullOrEmpty(normalized) ? null : normalized.toLowerCase();
    }

    private void mergeTuples() {
        Map<String, Collection<Tuple>> map = normalizedMap.asMap();
        Comparator<Tuple> compByAddrLength = (t1, t2) -> Integer.compare(getEmailAddr(t1).length(), getEmailAddr(t2).length());
        for (Collection<Tuple> tuples: map.values()) {
            if (tuples.size() == 1) {
                merged.push(tuples.iterator().next());
            } else {
                if (ZimbraLog.contact.isDebugEnabled()) {
                    String joined = Joiner.on(", ").join(tuples.stream().map(t -> getEmailAddr(t)).collect(Collectors.toList()));
                    ZimbraLog.contact.debug("merging related contacts %s", joined);
                }
                String longestAddr = getEmailAddr(tuples.stream().max(compByAddrLength).get());
                long totalCount = tuples.stream().collect(Collectors.summingLong(t -> getCount(t)));
                Tuple mergedTuple = tuples.iterator().next().clone();
                mergedTuple.put(addrField, longestAddr);
                mergedTuple.put(countField, totalCount);
                merged.push(mergedTuple);
            }
        }
    }

    @Override
    public void open() throws IOException {
        stream.open();
        Tuple tuple = stream.read();
        while(true) {
            if (tuple.EOF) {
                EOF = tuple;
                break;
            }

          String normalized = normalizeEmail(getEmailAddr(tuple));
          if (normalized != null && !filterValues.contains(normalized)) {
              normalizedMap.put(normalized, tuple);
          }
          tuple = stream.read();
        }
        mergeTuples();
    }

    @Override
    public void close() throws IOException {
        stream.close();
        normalizedMap.clear();
    }

    @Override
    public Tuple read() throws IOException {
        if (merged.isEmpty()) {
            return EOF;
        } else{
            return merged.pop();
        }
    }

    @Override
    public StreamComparator getStreamSort() {
        return stream.getStreamSort();
    }

    @Override
    public Explanation toExplanation(StreamFactory factory) throws IOException {
        return new StreamExplanation(getStreamNodeId().toString())
        .withChildren(new Explanation[]{
          stream.toExplanation(factory)
        })
        .withFunctionName(factory.getFunctionName(this.getClass()))
        .withImplementingClass(this.getClass().getName())
        .withExpressionType(ExpressionType.STREAM_DECORATOR)
        .withExpression(toExpression(factory, false).toString());
    }
}
