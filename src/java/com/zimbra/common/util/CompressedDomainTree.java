/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 25, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.common.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CompressedDomainTree {
    
    private TreeMap mRoot;
    
    private static final String SEP = "\\.";
    private static final String LEAF = "<>";
    
    public CompressedDomainTree() {
        mRoot = new TreeMap();
    }
    
    private void addNodes(Map parent, String[] domains, int index) {
        String d = domains[index];
        Object o = parent.get(d);
        if (o == null) {
            if (index > 0) {
                Map child = new TreeMap();
                parent.put(d, child);
                addNodes(child, domains, --index);
            } else {
                parent.put(d, d);
            }
        } else if (o instanceof String) {
            if (index > 0) {
                // upgrade to a map
                Map child = new TreeMap();
                addNodes(child, domains, --index);
                parent.put(d, child);
                // special child to indicate this domain
                // is also a "leaf".
                child.put(LEAF, d);
            } else {
                // nothing to do, already in the map
            }
        } else if (o instanceof Map) {
            Map child = (Map) o;
            if (index > 0) {
                // this branch already present
                addNodes(child, domains, --index);
            } else {
                // special child to indicate this domain
                // is also a "leaf".
                child.put(LEAF, d);
            }
        }
    }

    public void add(String domain) {
        domain = domain.toLowerCase();
        String[] domains;
        if (domain.charAt(0) == '[') {
        	// special case [n.n.n.n] as we don't want to split it up
        	domains = new String[] { domain };
        } else {
        	domains = domain.split(SEP);
        }
        if (domains == null || domains.length == 0)
            return;
        addNodes(mRoot, domains, domains.length-1);
    }
    
    public boolean hasNode(String node) {
        return false;
    }
    
    public static boolean hasNode(String node, String tree, char seperator) {
        return false;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        dump(mRoot, "", sb);
        return sb.toString();
    }
    
    /**
     * @param root
     */
    private void dump(Map root, String indent, StringBuffer sb) {
        for (Iterator it = root.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            String name = (String) entry.getKey();
            Object o = entry.getValue();
            if (o instanceof String) {
                //sb.append(indent).append(name).append(": ").append(o).append("\n");
                sb.append(indent).append(name).append("\n");
            } else {
                sb.append(indent).append(name).append(": {\n");
                dump((Map) o, indent+" ", sb);
                sb.append(indent).append("}\n");
            }
            
        }
    }

    public String toTree() {
        StringBuffer sb = new StringBuffer();
        dumpTree(mRoot, sb);
        return sb.toString();
    }
    
    /**
     * @param root
     */
    private void dumpTree(Map root, StringBuffer sb) {
        boolean first = true;
        for (Iterator it = root.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            String name = (String) entry.getKey();
            Object o = entry.getValue();
            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }
            if (o instanceof String) {
                //sb.append(indent).append(name).append(": ").append(o).append("\n");
                if (name.equals(LEAF)) {
                    //sb.append('');
                } else {
                    sb.append(name);
                }
            } else {
                sb.append(name).append('{');
                dumpTree((Map) o,sb);
                sb.append('}');
            }
            
            
        }
    }

    public static void main(String args[]) throws AddressException {
        CompressedDomainTree dt = new CompressedDomainTree();
        String domains[] = {
                "something", "[127.0.0.1]",
                "stanford.edu", "windlord.stanford.edu", "lists.stanford.edu",
                "washington.edu",
                "andrew.cmu.edu", "mit.edu", "alumni.mit.edu", "yahoo.com", "hotmail.com"
        };
        StringBuffer full = new StringBuffer();
        for (int i=0; i < domains.length; i++) {
            if (i>0)
                full.append(",");
            full.append(domains[i]);
            dt.add(domains[i]);
        }

        System.out.println(dt.toTree());
        System.out.println(full.toString());
        InternetAddress ia = new InternetAddress("schemers@[12.12.12.12]");
        System.out.println(ia.getAddress());
        System.out.println(ia.getPersonal());
        System.out.println(ia.getType());
    }
    
    public List getNodes() {
        return null;
    }
    
    public static List getNodes(String tree, char seperator) {
        return null;
    }
}
