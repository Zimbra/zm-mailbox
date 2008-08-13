/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.im.IMPresence.Show;

/**
 * 
 * 
 * 
 * 
 */
class PresencePriorityMap {
    
    private static class ResourcePresence {
        public String resource;
        public IMPresence presence;
        public ResourcePresence(String resource, IMPresence presence) {
            this.resource = resource; 
            this.presence = presence;
        }
        public String toString() {
            return (resource == null ? "" : resource) + ": "+presence.toString();
        }
    }
    
    HashMap<Integer, List<ResourcePresence>> mPriorityMap = 
        new HashMap<Integer, List<ResourcePresence>>();
    
    boolean isEmpty() {
        return mPriorityMap.isEmpty();
    }
    
    void addPresenceUpdate(String resource, IMPresence pres) {
        if (resource == null)
            resource = "";
        removePresence(resource);
        List<ResourcePresence> list = mPriorityMap.get((int)pres.getPriority());
        if (list == null) {
            list = new ArrayList<ResourcePresence>();
            mPriorityMap.put((int)pres.getPriority(), list);
        }
        list.add(new ResourcePresence(resource, pres));
    }

    void removePresence(String resource) {
        if (resource == null)
            resource = "";
        
        // find all previous presence entries from this resource, delete them
        // clean up any empty resource lists
        for (Iterator<List<ResourcePresence>> listIter = mPriorityMap.values().iterator(); listIter.hasNext();) {
            List<ResourcePresence> list = (List<ResourcePresence>)listIter.next();
            for (Iterator<ResourcePresence> rpIter = list.iterator(); rpIter.hasNext();) {
                ResourcePresence rp = (ResourcePresence)rpIter.next();
                if (rp.resource.equals(resource)) {
                    rpIter.remove();
                }
            }
            if (list.size() == 0) {
                listIter.remove();
            }
        }
    }
    
    IMPresence getEffectivePresence() {
        ArrayList<Integer> priorities = new ArrayList<Integer>();
        priorities.addAll(mPriorityMap.keySet());
        Collections.sort(priorities);
        if (priorities.size() > 0) {
            int priority = priorities.get(priorities.size()-1);
            List<ResourcePresence> list = mPriorityMap.get(priority);
            
            ResourcePresence toRet = null;

            for (ResourcePresence rp : list) {
                // prefer entries with a blank resource.  Otherwise take the last one in the list.
                if (rp.resource.length() == 0) {
                    toRet = rp;
                    break;
                }
                toRet = rp;
            }
            
            if (toRet != null) {
                return toRet.presence;
            }
        }
        
        // unavailable!
        return IMPresence.UNAVAILABLE;
    }
    
    public static void main(String[] args) {
        PresencePriorityMap map = new PresencePriorityMap();
        
        IMPresence check;
        check = map.getEffectivePresence();
        assert(check.getShow() == Show.OFFLINE);
        
        map.addPresenceUpdate("foo", new IMPresence(Show.AWAY, (byte)0, ""));
        map.addPresenceUpdate("bar", new IMPresence(Show.CHAT, (byte)0, ""));
        check = map.getEffectivePresence();
        assert(check.getShow() == Show.CHAT);
        assert(check.getPriority() == 0);
        
        map.addPresenceUpdate("foo", new IMPresence(Show.DND, (byte)1, ""));
        check = map.getEffectivePresence();
        assert(check.getShow() == Show.DND);
        assert(check.getPriority() == 1);
        
        map.removePresence("foo");
        check = map.getEffectivePresence();
        assert(check.getShow() == Show.CHAT);
        assert(check.getPriority() == 0);
        
        map.removePresence("bar");
        check = map.getEffectivePresence();
        assert(check.getShow() == Show.OFFLINE);
        assert(check.getPriority() == 0);
    }
}
