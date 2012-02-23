package com.zimbra.cs.service.admin;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultimap;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DirectoryEntryVisitor;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

public class SearchAutoProvDirectory extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        String query = request.getAttribute(AdminConstants.E_QUERY, null);
        String name = request.getAttribute(AdminConstants.E_NAME, null);
        
        if ((query != null) && (name != null)) {
            throw ServiceException.INVALID_REQUEST("only one of filter or name can be provided", null);
        }

        int maxResults = (int) request.getAttributeLong(
                AdminConstants.A_MAX_RESULTS, SearchDirectory.MAX_SEARCH_RESULTS);
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, Integer.MAX_VALUE);
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        boolean refresh = request.getAttributeBool(AdminConstants.A_REFRESH, false);
        
        String keyAttr = request.getAttribute(AdminConstants.A_KEYATTR);
        
        String attrsStr = request.getAttribute(AdminConstants.A_ATTRS, null);
        String[] returnAttrs = null;
        if (attrsStr != null) {
            Set<String> attrs = new HashSet<String>();
            for (String attr : Splitter.on(',').trimResults().split(attrsStr)) {
                attrs.add(attr);
            }
            if (!attrs.contains(keyAttr)) {
                attrs.add(keyAttr);
            }
            returnAttrs = attrs.toArray(new String[attrs.size()]);
        }
        
        Element eDomain = request.getElement(AdminConstants.E_DOMAIN);
        DomainBy domainBy = DomainBy.fromString(eDomain.getAttribute(AdminConstants.A_BY));
        String domainValue = eDomain.getText();
        Domain domain = prov.get(domainBy, domainValue);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainValue);
        }
        
        checkRight(zsc, context, domain, Admin.R_autoProvisionAccount);
        
        AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        
        List<Entry> entryList = null;
        if (session != null) {
            Cache.Params params = new Cache.Params(domain, query, name, keyAttr,
                    returnAttrs, maxResults);
            
            if (!refresh) {
                entryList = Cache.getFromCache(session, params);
            }
            if (entryList == null) {
                entryList = search(domain, query, name, keyAttr, returnAttrs, maxResults);
                Cache.putInCache(session, params, entryList);
            }

        } else {
            entryList = search(domain, query, name, keyAttr, returnAttrs, maxResults);
        }
        
        Element response = zsc.createElement(AdminConstants.SEARCH_AUTO_PROV_DIRECTORY_RESPONSE);
        encodeEntries(response, entryList, keyAttr, offset, limit);
        return response;
    }
    
    private static class Cache {
        private static final String SEARCH_AUTO_PROV_DIRECTORY_CACHE_KEY = "SearchAutoProvDirectory";
        
        private static class Params {
            Domain domain;
            String query;
            String name;
            String keyAttr;
            String[] returnAttrs; 
            int maxResults;
            
            private Params(Domain domain, String query, String name, String keyAttr,
                    String[] returnAttrs, int maxResults) {
                this.domain = domain;
                this.query = query;
                this.name = name;
                this.keyAttr = keyAttr;
                this.returnAttrs = returnAttrs; 
                this.maxResults = maxResults;
            }
            
            @Override
            public boolean equals(Object other) {
                if (other instanceof Params) {
                    Params otherParams = (Params) other;
                    
                    if (!domain.getId().equals(otherParams.domain.getId())) {
                        return false;
                    }
                    
                    if (!equals(query, otherParams.query)) {
                        return false;
                    }
                    
                    if (!equals(name, otherParams.name)) {
                        return false;
                    }
                    
                    if (!equals(keyAttr, otherParams.keyAttr)) {
                        return false;
                    }
                    
                    if (maxResults != otherParams.maxResults) {
                        return false;
                    }
                    
                    if (!equals(returnAttrs, otherParams.returnAttrs)) {
                        return false;
                    }
                    
                    return true;
                } else {
                    return false;
                }
            }
            
            private boolean equals(String s1, String s2) {
                if (s1 == null) {
                    return s2 == null;
                } else {
                    return s1.equals(s2);
                }
            }
            
            private boolean equals(String[] s1, String[] s2) {
                if (s1 == null) {
                    return s2 == null;
                } else {
                    if (s2 == null) {
                        return false;
                    } else {
                        if (s1.length != s2.length) {
                            return false;
                        }
                        Set<String> set1 = new HashSet(Arrays.asList(s1));
                        Set<String> set2 = new HashSet(Arrays.asList(s2));
                        return set1.equals(set2);
                    }
                }
            }
        }
        
        private static class CachedResult {
            private Params params;
            private List<Entry> result;
            
            private CachedResult(Params params, List<Entry> result) {
                this.params = params;
                this.result = result;
            }
        }     
        
        private static List<Entry> getFromCache(AdminSession session, Params params) {
            Object cached = session.getData(SEARCH_AUTO_PROV_DIRECTORY_CACHE_KEY);
            if (cached == null) {
                return null;
            }
            
            CachedResult cachedResult = (CachedResult) cached;
            if (cachedResult.params.equals(params)) {
                return cachedResult.result;
            }
            
            return null;
        }
        
        private static void putInCache(AdminSession session, Params params, List<Entry> result) {
            CachedResult cachedResult = new CachedResult(params, result);
            session.setData(SEARCH_AUTO_PROV_DIRECTORY_CACHE_KEY, cachedResult);
        }
    };
    
    
    private void encodeEntries(Element response, List<Entry> entryList, String keyAttr, 
            int offset, int limit) {
        int totalEntries = entryList.size();
        
        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < totalEntries; i++) { 
            Entry entry = entryList.get(i);
            
            Element eEntry = response.addElement(AdminConstants.E_ENTRY);
            eEntry.addAttribute(AdminConstants.A_DN, entry.dn);
            
            Object keyValue = entry.attrs.get(keyAttr);
            if (keyValue != null) {
                encodeKey(eEntry, keyValue);
            }
            
            for (Map.Entry<String, Object> attr : entry.attrs.entrySet()) {
                String attrName = attr.getKey();
                if (!attrName.equals(keyAttr)) {
                    encodeAttr(eEntry, attrName, attr.getValue());
                }
            }
        }
        
        response.addAttribute(AdminConstants.A_MORE, i < totalEntries);
        response.addAttribute(AdminConstants.A_SEARCH_TOTAL, totalEntries);
    }
    
    private void encodeKey(Element parent, Object value) {
        if (value instanceof String) {
            parent.addElement(AdminConstants.E_KEY).setText((String) value);
        } if (value instanceof String[]) {
            for (String v : (String[]) value) {
                parent.addElement(AdminConstants.E_KEY).setText(v);
            }
        }
    }
    
    private void encodeAttr(Element parent, String attrName, Object value) {
        if (value instanceof String) {
            parent.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, attrName).setText((String) value);
        } else {
            for (String v : (String[]) value) {
                parent.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, attrName).setText(v);
            }
        }
    }
    
    private static class Entry implements Comparable<Entry> {
        private String dn;
        private Map<String, Object> attrs;
        
        private Entry(String dn, Map<String, Object> attrs) {
            this.dn = dn;
            this.attrs = attrs;
        }
        
        /*
         * called from TreeMultimap when two entries ahs the same key
         *
         * Note: must not return 0 here, if we do, the entry will not be 
         *       inserted into the TreeMultimap.
         */
        @Override
        public int compareTo(Entry other) {
            return 1;
        }
    }
    
    private static class Result implements DirectoryEntryVisitor {
        private String keyAttr;
        
        // for sorting
        private TreeMultimap<String, Entry> entries = TreeMultimap.create();
                
        private Result(String keyAttr) {
            this.keyAttr = keyAttr;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs) {
            String key = null;
            Object value = attrs.get(keyAttr);
            if (value instanceof String) {
                key = (String) value;
            } else if (value instanceof String[]) {
                String[] values = (String[]) value;
                if (values.length > 1) {
                    key = values[0];
                }
            }
            
            if (key == null) {
                key = "";
            }
            
            entries.put(key, new Entry(dn, attrs));
        }
        
        private Collection<Entry> getEntries() {
            return entries.values();
        }
        
        private int size() {
            return entries.size();
        }
    };
    
    private List<Entry> search(Domain domain, String query, String name, String keyAttr, 
            String[] returnAttrs, int maxResults) throws ServiceException {
        
        Result result = new Result(keyAttr);
        
        Provisioning.getInstance().searchAutoProvDirectory(domain, query, name, 
                returnAttrs, maxResults, result);
           
        List<Entry> entryList = Lists.newArrayListWithExpectedSize(result.size());
        entryList.addAll(result.getEntries());
        return entryList;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_autoProvisionAccount);
    }
}
