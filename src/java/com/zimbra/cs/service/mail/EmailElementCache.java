/*
 * Created on Jun 14, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.HashMap;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import com.liquidsys.coco.mime.ParsedAddress;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.util.StringUtil;

/**
 * @author schemers
 */
public class EmailElementCache {

	public static final int EMAIL_TYPE_NONE = 0;
    public static final int EMAIL_TYPE_FROM = 1;
    public static final int EMAIL_TYPE_TO = 2;
    public static final int EMAIL_TYPE_CC = 3;
    public static final int EMAIL_TYPE_BCC = 4;
    public static final int EMAIL_TYPE_REPLY_TO = 5;
    public static final int EMAIL_TYPE_SENDER = 6;

	public int mId;
	public HashMap mCache;

    public static class CacheNode extends ParsedAddress {
        public String id;

        public CacheNode(String address, int newId) {
            super(address);
            id = Integer.toString(newId);
        }
        public CacheNode(InternetAddress ia, int newId) {
            super(ia);
            id = Integer.toString(newId);
        }
        public CacheNode(String email, String personal, int newId) {
            super(email, personal);
            id = Integer.toString(newId);
        }
        public CacheNode(CacheNode node, int newId) {
            super(node);
            id = Integer.toString(newId);
        }
    }

	public EmailElementCache() {
		mId = 0;
		mCache = new HashMap();
	}

	public CacheNode add(String address, Set unique, boolean matchEmail) {
        if (address == null)
            return null;
        address = StringUtil.stripControlCharacters(address).trim();
        if (address.equals(""))
            return null;

        if (unique != null && unique.contains(address))
            return null;

        CacheNode node = (CacheNode) mCache.get(address);
        boolean nodePresent = node != null;
        if (!nodePresent)
            node = new CacheNode(address, mId++);

        if (unique != null && !address.equals(node.emailPart))
            unique.add(address);
        node = addImplementation(node, unique, matchEmail, nodePresent);
        if (!nodePresent && node != null)
            mCache.put(address, node);
        return node;
    }
    public CacheNode add(InternetAddress ia, Set unique, boolean matchEmail) {
        if (ia == null)
            return null;

        CacheNode node = null;
        if (ia.getAddress() != null)
            node = (CacheNode) mCache.get(ia.getAddress());
        boolean nodePresent = node != null;
        if (!nodePresent)
            node = new CacheNode(ia, mId++);

        return addImplementation(node, unique, matchEmail, nodePresent);
    }
    public CacheNode add(CacheNode externalNode, Set unique, boolean matchEmail) {
        if (externalNode == null)
            return null;

        CacheNode node = null;
        if (externalNode.emailPart != null)
        	node = (CacheNode) mCache.get(externalNode.emailPart);
        boolean nodePresent = node != null;
        if (!nodePresent)
            node = new CacheNode(externalNode, mId++);

        return addImplementation(node, unique, matchEmail, nodePresent);
    }
    
    private CacheNode addImplementation(CacheNode node, Set unique, boolean matchEmail, boolean nodePresent) {
        if (unique != null && node.emailPart != null) {
            if (unique.contains(node.emailPart))
                return null;
            unique.add(node.emailPart);
        }

        if (!nodePresent && matchEmail && node.emailPart != null) {
            if (mCache.containsKey(node.emailPart))
                node = (CacheNode) mCache.get(node.emailPart);
            else
                mCache.put(node.emailPart, node);
        }

        return node;
    }

    public Element makeEmail(Element parent, String address, int type, Set unique) {
        CacheNode node = add(address, unique, false);
        if (node == null)
            return null;
        return encode(parent, node, type);
    }
    public Element makeEmail(Element parent, InternetAddress ia, int type, Set unique) {
        CacheNode node = add(ia, unique, false);
        if (node == null)
            return null;
        return encode(parent, node, type);
    }
    public Element makeEmail(Element parent, CacheNode externalNode, int type, Set unique) {
        CacheNode node = add(externalNode, unique, false);
        if (node == null)
            return null;
        return encode(parent, node, type);
    }
    
    private Element encode(Element parent, CacheNode node, int type) {
        Element elem = parent.addElement(MailService.E_EMAIL);
        if (node.first) {
            node.parse();
            node.first = false;
            elem.addAttribute(MailService.A_ID, node.id);
            if (node.emailPart != null)
                elem.addAttribute(MailService.A_ADDRESS, node.emailPart);
            if (node.firstName != null)
                elem.addAttribute(MailService.A_DISPLAY, node.firstName);
            if (node.personalPart != null)
                elem.addAttribute(MailService.A_PERSONAL, node.personalPart);
        } else {
            elem.addAttribute(MailService.A_REF, node.id);
        }

        String t = null;
        switch (type) {
            case EMAIL_TYPE_FROM:      t = "f";  break;
            case EMAIL_TYPE_SENDER:    t = "s";  break;
            case EMAIL_TYPE_TO:        t = "t";  break;
            case EMAIL_TYPE_REPLY_TO:  t = "r";  break;
            case EMAIL_TYPE_CC:        t = "c";  break;
            case EMAIL_TYPE_BCC:       t = "b";  break;
        }
        if (t != null)
            elem.addAttribute(MailService.A_ADDRESS_TYPE, t);
        return elem;
    }
}
