/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.doc.soap;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;


/**
 * This class represents the root data model for the SOAP API.
 */
public class Root {

    private List<Service> services = Lists.newLinkedList();

    Root() {
    }

    public Service addService(Service service) {
        this.services.add(service);
        return service;
    }

    public Service getServiceForNamespace(String namespace) {
        for (Service svc : services) {
            if (svc.getNamespace().equals(namespace)) {
                return svc;
            }
        }
        return null;
    }

    public List<Service> getServices() {
        Collections.sort(this.services, new Service.ServiceComparator());
        return Collections.unmodifiableList(this.services);
    }

    /**
     * Gets a list of all commands in all services.
     */
    public List<Command> getAllCommands() {
        List<Command> allCommands = Lists.newLinkedList();

        Iterator<Service> sit = this.getServices().iterator();
        while (sit.hasNext()) {
            Service    s = sit.next();
            Iterator<Command> cit = s.getCommands().iterator();
            while (cit.hasNext()) {
                Command    c = cit.next();
                allCommands.add(c);
            }
        }

        Collections.sort(allCommands, new Command.CommandComparator());
        return allCommands;
    }

    public void dump() {
        this.dump(false);
    }

    public void dump(boolean dumpCommands) {
        System.out.println("Dump doc root...");
        System.out.println(this);

        System.out.println("Dump services...");
        Iterator it = this.services.iterator();
        while (it.hasNext()) {
            Service s = (Service)it.next();
            s.dump(dumpCommands);
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[docroot;hashCode=").append(hashCode());
        buf.append(";serviceCount=").append(this.services.size());
        buf.append("]");
        return    buf.toString();
    }
} // end Root class
