/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.account;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Config extends Entry {

    public boolean isInheritedAccountAttr(String name);

    public boolean isInheritedDomainAttr(String name);

    public boolean isInheritedServerAttr(String name);
}

