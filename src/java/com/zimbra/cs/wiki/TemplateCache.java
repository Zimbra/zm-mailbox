/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.wiki.WikiTemplate.Context;

public class TemplateCache {
	private static final int DEFAULT_CACHE_SIZE = 1000;
	private LRUMap mCache;
	private static final String SEP = ":";
	
	public TemplateCache() {
		this(DEFAULT_CACHE_SIZE);
	}
	public TemplateCache(int cacheSize) {
		mCache = new LRUMap(cacheSize);
	}
	
	public synchronized void addTemplate(Context ctxt, WikiTemplate template, String val) {
		String key = generateKey(ctxt, template);
		mCache.put(key, val);
	}
	
	public synchronized String getTemplate(Context ctxt, WikiTemplate template) {
		String key = generateKey(ctxt, template);
		return (String)mCache.get(key);
	}
	
	private String generateKey(Context ctxt, WikiTemplate template) {
		List<WikiTemplate> inclusions = new ArrayList<WikiTemplate>();
		template.getInclusions(ctxt, inclusions);
		Collections.sort(inclusions);
		StringBuilder name = new StringBuilder();
		name.append(template.getId()).append(SEP).append(template.getKey()).append(SEP);
		for (WikiTemplate t : inclusions) {
			name.append(t.getId()).append(SEP).append(t.getKey()).append(SEP);
		}
		return name.toString();
	}
}
