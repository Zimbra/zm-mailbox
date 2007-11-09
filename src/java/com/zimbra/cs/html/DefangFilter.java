/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.html;

/* 
 * (C) Copyright 2002-2004, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.filters.DefaultFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * @author schemers@example.zimbra.com
 * 
 * very Mutated version of ElementRemover.java filter from cyberneko html.
 * change accepted/removed elements to static hashmaps for one-time 
 * initialization, switched from Hashtable to HashMap, sanatize
 * attributes, etc. 
 * 
 * TODO: more checks:
 * allow limited use of <meta> tags? like for Content-Type?
 * make sure any clicked links pop up in new window 
 * figure out how to block images by default, and how to re-enable them. styles?  
 * strict attr value checking?
 *  don't allow id attr in tags if we aren't putting html into an iframe (I'm assuming we are, and id's in iframes don't conflict with iframes elsewhere)
 * 
 *  
 * MAYBE:
 *  allow style but strip out /url(.*)/? Might have other reasons to leave it 
 * 
 */
public class DefangFilter extends DefaultFilter {

    /**
     * disable all form/input type tags
     */
    private static final boolean ENABLE_INPUT_TAGS = true;
    
    /**
     * enable table tags
     */
    private static final boolean ENABLE_TABLE_TAGS = true;

    /**
     * enable phrase tags (EM, STRONG, CITE, DFN, CODE, SAMP, KBD, VAR, ABBR, ACRONYM)
     */
    private static final boolean ENABLE_PHRASE_TAGS = true;
    
    /**
     * enable list tags (UL, OL, LI, DL, DT, DD, DIR, MENU)
     */
    private static final boolean ENABLE_LIST_TAGS = true;

    /**
     * enable font style tags (TT, I, B, BIG, SMALL, STRIKE, S, U) 
     */
    private static final boolean ENABLE_FONT_STYLE_TAGS = true;
    
    //
    // Constants
    //

    /** A "null" object. */
    protected static final Object NULL = new Object();

    // regexes inside of attr values to strip out
    private static final Pattern AV_JS_ENTITY = Pattern.compile("&\\{[^}]*\\}");
    private static final Pattern AV_SCRIPT_TAG = Pattern.compile("</?script/?>", Pattern.CASE_INSENSITIVE);
    
    // regex for URLs href. TODO: beef this up
    private static final Pattern VALID_URL = Pattern.compile("^(https?://[\\w-].*|mailto:.*|cid.*:|[^:].*)$", Pattern.CASE_INSENSITIVE);

    //
    // Data
    //

    // information

    /** attr Set cache */
    private static HashMap mAttrSetCache = new HashMap();

    /** Accepted elements. */
    private static HashMap mAcceptedElements = new HashMap();

    /** Removed elements. */
    private static HashMap mRemovedElements = new HashMap();

    // state

    private String mBaseHref = null;

    /** Strip images */
    boolean mNeuterImages;
    
    /** The element depth. */
    protected int mElementDepth;

    /** The element depth at element removal. */
    protected int mRemovalElementDepth;

    /** The style element depth */
    protected int mStyleDepth;

    //private static String[] STD_CORE = { "id", "class", "title", "style" };
    private static String CORE = "id,class,title,style,";
    private static String LANG = "dir,lang,xml:lang,";
    private static String CORE_LANG = CORE+LANG;
    private static String KBD = "accesskey,tabindex,";

    static {    
        // set which elements to accept
        acceptElement("a", CORE+KBD+",charset,coords,href,hreflang,name,rel,rev,shape,target,type");
        acceptElement("address", CORE_LANG);
        //acceptElement("base", "href"); //,target");
        acceptElement("bdo", CORE_LANG);
        acceptElement("blockquote", CORE_LANG+"cite");
        acceptElement("body", CORE_LANG); //+"alink,background,bgcolor,link,text,vlink");
        acceptElement("br", CORE+"clear");
        acceptElement("center", CORE_LANG);
        acceptElement("del", CORE_LANG+"cite,datetime");
        acceptElement("div", CORE_LANG+"align");
        acceptElement("head", LANG); // profile attr removed
        acceptElement("h1", CORE_LANG+"align");
        acceptElement("h2", CORE_LANG+"align");
        acceptElement("h3", CORE_LANG+"align");
        acceptElement("h4", CORE_LANG+"align");
        acceptElement("h5", CORE_LANG+"align");
        acceptElement("h6", CORE_LANG+"align");
        acceptElement("hr", CORE_LANG+"align,noshade,size,width");
        acceptElement("html", LANG+"xmlns");
        acceptElement("img", CORE_LANG+"align,alt,border,height,hspace,ismap,longdesc,src,usemap,vspace,width");
        acceptElement("ins", CORE_LANG+"cite");
        acceptElement("label", CORE_LANG+"for");
        //acceptElement("link", CORE_LANG+"charset,href,hreflang,media,ntarget,rel,rev,type");

        // NOTE: comment out noframes so its text shows up, since we are nuke frame-related tags
        //acceptElement("noframes", CORE_LANG);
        // NOTE: comment out noscript so its text shows up, since we are nuking script tags
        //acceptElement("noscript", CORE_LANG); // maybe convert to always execute if we are stripping script?
        acceptElement("p", CORE_LANG+"align");
        acceptElement("pre", CORE_LANG+"width");
        acceptElement("q", CORE_LANG+"cite");
        acceptElement("span", CORE_LANG);

        // style removed. TODO: see if we can safely include it or not, maybe by sanatizing
        acceptElement("style", CORE_LANG);
        acceptElement("sub",  CORE_LANG);
        acceptElement("sup",  CORE_LANG);

        //acceptElement("title", CORE_LANG);
        acceptElement("title", "");
        
        if (ENABLE_FONT_STYLE_TAGS) {
            acceptElement("b",  CORE_LANG);
            acceptElement("basefont", CORE_LANG+"color,face,size");
            acceptElement("big", CORE_LANG);
            acceptElement("font", CORE_LANG+"color,face,size");
            acceptElement("i", CORE_LANG);
            acceptElement("s", CORE_LANG);
            acceptElement("small", CORE_LANG);
            acceptElement("strike", CORE_LANG);
            acceptElement("tt", CORE_LANG);
            acceptElement("u", CORE_LANG);
        } else {
            // allow the text, just strip the tags
        }
        
        if (ENABLE_LIST_TAGS) {
            acceptElement("dir", CORE_LANG+"compact");
            acceptElement("dl", CORE_LANG);
            acceptElement("dt", CORE_LANG);
            acceptElement("li", CORE_LANG+"type,value");
            acceptElement("ol", CORE_LANG+"compact,start,type");
            acceptElement("ul", CORE_LANG+"compact,type");
            acceptElement("dd", CORE_LANG);
            acceptElement("menu", CORE_LANG+"compact");
        } else {
            // allow the text, just strip the tags
        }
        
        if (ENABLE_PHRASE_TAGS) {
            acceptElement("abbr", CORE_LANG);
            acceptElement("acronym", CORE_LANG);
            acceptElement("cite", CORE_LANG);
            acceptElement("code", CORE_LANG);
            acceptElement("dfn", CORE_LANG);
            acceptElement("em", CORE_LANG);
            acceptElement("kbd", CORE_LANG);
            acceptElement("samp", CORE_LANG);
            acceptElement("strong", CORE_LANG);
            acceptElement("var", CORE_LANG);
        } else {
            // allow the text, just strip the tags
        }

        if (ENABLE_TABLE_TAGS) {
            acceptElement("caption", CORE_LANG+"align");
            acceptElement("col",CORE_LANG+"alink,char,charoff,span,valign,width");
            acceptElement("colgroup", CORE_LANG+"alink,char,charoff,span,valign,width");
            acceptElement("table", CORE_LANG+"align,valign,bgcolor,border,cellpadding,cellspacing,frame,rules,summary,width");
            acceptElement("tbody", CORE_LANG+"align,char,charoff,valign");
            acceptElement("td", CORE_LANG+"abbr,align,axis,bgcolor,char,charoff,colspan,headers,height,nowrap,rowspan,scope,,valign,width");
            acceptElement("tfoot", CORE_LANG+"align,char,charoff,valign");
            acceptElement("th", CORE_LANG+"abbr,align,axis,bgcolor,char,charoff,colspan,headers,height,nowrap,rowspan,scope,valign,width");
            acceptElement("thead", CORE_LANG+"align,char,charoff,valign");
            acceptElement("tr", CORE_LANG+"align,bgcolor,char,charoff,valign");
        } else {
            // allow the text, just strip the tags
        }

        
        if (ENABLE_INPUT_TAGS) {
            acceptElement("area", CORE_LANG+KBD+"alt,coords,href,nohref,shape,target");
            acceptElement("button", CORE_LANG+KBD+"disabled,name,type,value");
            acceptElement("fieldset", CORE_LANG);
            acceptElement("form", CORE_LANG+"action,accept,acceptcharset,enctype,method,name,target");
            acceptElement("input", CORE_LANG+"accept,align,alt,checked,disabled,maxlength,name,readonly,size,src,type,value");
            acceptElement("legend", CORE_LANG+"align");
            acceptElement("map", CORE_LANG+"name");
            acceptElement("optgroup", CORE_LANG+"disabled,label");
            acceptElement("option", CORE_LANG+KBD+"disabled,label,selected,value");
            acceptElement("select", CORE_LANG+KBD+"disabled,multiple,name,size");
            acceptElement("textarea", CORE_LANG+"cols,disabled,name,readonly,rows");
        } else {
            removeElement("area");
            removeElement("button");
            removeElement("fieldset");
            removeElement("form");
            removeElement("input");
            removeElement("legend");
            removeElement("map");
            removeElement("optgroup");
            removeElement("option");
            removeElement("select");
            removeElement("textarea");
        }
        
        // completely remove these elements and all enclosing tags/text
        removeElement("applet");
        removeElement("frame");
        removeElement("frameset");
        removeElement("iframe");
        removeElement("object");
        removeElement("script");
        removeElement("style");
        
        // don't remove "content" of these tags since they have none.
        //removeElement("meta");
        //removeElement("param");        
        
    }
    
    /**
     * @param neuterImages
     */
    public DefangFilter(boolean neuterImages) {
        mNeuterImages = neuterImages;
    }

    /** 
     * Specifies that the given element should be accepted and, optionally,
     * which attributes of that element should be kept.
     *
     * @param element The element to accept.
     * @param attributes The comma-seperated list of attributes to be kept or null if no
     *                   attributes should be kept for this element.
     *
     * see #removeElement
     */
    public static void acceptElement(String element, String attributes) {
        element = element.toLowerCase();
        HashSet set = (HashSet) mAttrSetCache.get(attributes);
        if (set != null) {
            //System.out.println(element+" cached set "+set.size());
            mAcceptedElements.put(element, set);
            return;
        }
        set = new HashSet();
        String attrs[] = attributes.toLowerCase().split(",");
        if (attrs != null && attrs.length > 0) {
            for (int i=0; i < attrs.length; i++) {
                //System.out.println(element+"["+attrs[i]+"]");
                //deal with consecutive commas
                if (attrs[i].length() > 0)
                    set.add(attrs[i]);
            }
        }
        mAcceptedElements.put(element, set);
        mAttrSetCache.put(attributes, set);
    }

    /** 
     * Specifies that the given element should be completely removed. If an
     * element is encountered during processing that is on the remove list, 
     * the element's start and end tags as well as all of content contained
     * within the element will be removed from the processing stream.
     *
     * @param element The element to completely remove.
     */
    public static void removeElement(String element) {
        Object key = element.toLowerCase();
        Object value = NULL;
        mRemovedElements.put(key, value);
    } // removeElement(String)

    //
    // XMLDocumentHandler methods
    //

    // since Xerces-J 2.2.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, 
                              NamespaceContext nscontext, Augmentations augs) 
        throws XNIException {
        mElementDepth = 0;
        mRemovalElementDepth = Integer.MAX_VALUE;
        super.startDocument(locator, encoding, nscontext, augs);
    } // startDocument(XMLLocator,String,NamespaceContext,Augmentations)

    // old methods

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs)
        throws XNIException {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start prefix mapping. */
    public void startPrefixMapping(String prefix, String uri, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.startPrefixMapping(prefix, uri, augs);
        }
    } // startPrefixMapping(String,String,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth && handleOpenTag(element, attributes)) {
            super.startElement(element, attributes, augs);
        }
        mElementDepth++;
        if (element.localpart.equalsIgnoreCase("style")) mStyleDepth++;
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth && handleOpenTag(element, attributes)) {
            super.emptyElement(element, attributes, augs);
        }
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.comment(text, augs);
        }
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString,Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs) 
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            if (mStyleDepth > 0) {
                String result = text.toString().replaceAll("[uU][Rr][Ll]\\s*\\(.*\\)","url()");
                result = result.replaceAll("expression\\s*\\(.*\\)","");
                super.characters(new XMLString(result.toCharArray(), 0, result.length()), augs);    
            } else {
                super.characters(text, augs);
            }
        }
    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text, Augmentations augs) 
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.ignorableWhitespace(text, augs);
        }
    } // ignorableWhitespace(XMLString,Augmentations)

    /** Start general entity. */
    public void startGeneralEntity(String name, XMLResourceIdentifier id, String encoding, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.startGeneralEntity(name, id, encoding, augs);
        }
    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** Text declaration. */
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.textDecl(version, encoding, augs);
        }
    } // textDecl(String,String,Augmentations)

    /** End general entity. */
    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.endGeneralEntity(name, augs);
        }
    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.startCDATA(augs);
        }
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.endCDATA(augs);
        }
    } // endCDATA(Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth && elementAccepted(element.rawname)) {
            super.endElement(element, augs);
        }
        mElementDepth--;
        if (element.localpart.equalsIgnoreCase("style")) mStyleDepth--;
        if (mElementDepth == mRemovalElementDepth) {
            mRemovalElementDepth = Integer.MAX_VALUE;
        }
    } // endElement(QName,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
        if (mElementDepth <= mRemovalElementDepth) {
            super.endPrefixMapping(prefix, augs);
        }
    } // endPrefixMapping(String,Augmentations)

    //
    // Protected methods
    //

    /** Returns true if the specified element is accepted. */
    protected static boolean elementAccepted(String element) {
        Object key = element.toLowerCase();
        return mAcceptedElements.containsKey(key);
    } // elementAccepted(String):boolean

    /** Returns true if the specified element should be removed. */
    protected static boolean elementRemoved(String element) {
        Object key = element.toLowerCase();
        return mRemovedElements.containsKey(key);
    } // elementRemoved(String):boolean

    /** Handles an open tag. */
    protected boolean handleOpenTag(QName element, XMLAttributes attributes) {
        if (element.rawname.toLowerCase().equals("base")) {
            int index = attributes.getIndex("href");
            if (index != -1) {
                mBaseHref = attributes.getValue(index);
                if (mBaseHref != null && !mBaseHref.endsWith("/"))
                    mBaseHref += "/";
            }
        }
        if (elementAccepted(element.rawname)) {
            String eName = element.rawname.toLowerCase();
            Object value = mAcceptedElements.get(eName);
            if (value != NULL) {
                HashSet anames = (HashSet)value;
                int attributeCount = attributes.getLength();
                for (int i = 0; i < attributeCount; i++) {
                    String aName = attributes.getQName(i).toLowerCase();
                    if (!anames.contains(aName)) {
                        attributes.removeAttributeAt(i--);
                        attributeCount--;
                    } else {
                        sanatizeAttrValue(eName, aName, attributes, i);
                    }
                }
            }
            else {
                attributes.removeAllAttributes();
            }

            if (eName.equals("img")) {
                fixUrlBase(attributes, "src");
            } else if (eName.equals("a")) {
                fixUrlBase(attributes, "href");                
            }

            if (eName.equals("img") && mNeuterImages) {
                neuterImageTag(attributes);
            } else if (eName.equals("a")) {
                fixATag(attributes);
            }

            return true;
        }
        else if (elementRemoved(element.rawname)) {
            mRemovalElementDepth = mElementDepth;
        }
        return false;
    } // handleOpenTag(QName,XMLAttributes):boolean

    private void fixUrlBase(XMLAttributes attributes, String attrName) {
        int index = attributes.getIndex(attrName);
        if (index != -1) {
            String value = attributes.getValue(index);
            if (mBaseHref != null && value != null && value.indexOf(":") == -1) {
                attributes.setValue(index, mBaseHref+value);
            }
        }
    }

    /**
     * @param attributes
     */
    private void neuterImageTag(XMLAttributes attributes) {
        int dfIndex = attributes.getIndex("dfsrc");
        int srcIndex = attributes.getIndex("src");
        if (srcIndex != -1) {
            String srcValue = attributes.getValue(srcIndex);
            if (dfIndex != -1) {
                attributes.setValue(dfIndex, srcValue);
            } else {
                attributes.addAttribute(new QName("", "dfsrcf", "dfsrc", null), "DFSRC", srcValue);
            }
            attributes.removeAttributeAt(srcIndex);
            // remove dups if there are multiple src attributes
            srcIndex = attributes.getIndex("src");
            while (srcIndex != -1) {
                attributes.removeAttributeAt(srcIndex);
                srcIndex = attributes.getIndex("src");
            }
        }
    }

    /**
     * make sure all <a> tags have a target="_blank" attribute set.
     * @param attributes
     */
    private void fixATag(XMLAttributes attributes) {
	// BEGIN: bug 7927
	int index = attributes.getIndex("href");
	if (index == -1)	// links that don't have a href don't need target="_blank"
	    return;
	String href = attributes.getValue(index);
	if (href.indexOf('#') == 0) // LOCAL links don't need target="_blank"
	    return;
	// END: bug 7927
	index = attributes.getIndex("target");
        if (index != -1) {
            attributes.setValue(index, "_blank");
        } else {
            attributes.addAttribute(new QName("", "target", "target", null), "CDATA", "_blank");
        }
    }
    
    /**
     * sanatize an attr value. For now, this means stirpping out Java Script entity tags &{...},
     * and <script> tags.
     * 
     * 
     */
    private void sanatizeAttrValue(String eName, String aName, XMLAttributes attributes, int i) {
        String value = attributes.getValue(i);
        //System.out.println("==== "+eName+" "+aName+" ("+value+")");
        String result = AV_JS_ENTITY.matcher(value).replaceAll("JS-ENTITY-BLOCKED");
        result = AV_SCRIPT_TAG.matcher(result).replaceAll("SCRIPT-TAG-BLOCKED");
        // TODO: change to set?
        if (aName.equalsIgnoreCase("href") || aName.equalsIgnoreCase("src") || aName.equalsIgnoreCase("longdesc") || aName.equalsIgnoreCase("usemap")){
            if (!VALID_URL.matcher(result).matches()) {
                // TODO: just leave blank?
                result = "about:blank";
            }
        }
        if (aName.equalsIgnoreCase("style")) {
            result = value.replaceAll("/\\*.*\\*/","");
            result = result.replaceAll("[uU][Rr][Ll]\\s*\\(.*\\)","url()");
            result = result.replaceAll("expression\\s*\\(.*\\)","");
        }
        if (!result.equals(value)) {
            //System.out.println("**** "+eName+" "+aName+" ("+result+")");
            attributes.setValue(i, result);
        }
    }

} // class DefaultFilter
