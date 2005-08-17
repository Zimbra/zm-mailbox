package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public interface AttributeCallback {

    /**
     * called before an attribute is modified. If a ServiceException is thrown, no attributes will
     * be modified. The attrsToModify map should not be modified, other then for the current attrName
     * being called.
     * 
     * TODO: if dn/name/type is needed on a create (for whatever reason), we could consider passing
     * them in context with well-known-keys, or having separate *Create callbacks.
     * 
     * @param context place to stash data between invocations of pre/post
     * @param attrName name of the attribute being modified so the callback can be used with multiple attributes.
     * @param attrValue will be null, String, or String[]
     * @param attrsToModify a map of all the attributes being modified
     * @param entry entry object being modified. null if entry is being created.
     * @param isCreate set to true if called during create
     * @throws ServiceException causes the whole transaction to abort.
     */
    void preModify(
            Map context,
            String attrName,
            Object attrValue,
            Map attrsToModify,
            Entry entry,
            boolean isCreate) throws ServiceException;

    /**
     * called after a successful modify of the attributes. should not throw any exceptions.
     * 
     * @param context
     * @param attrName
     * @param entry Set on modify and create.
     * @param isCreate set to true if called during create
     */
    void postModify(
            Map context,
            String attrName,
            Entry entry,
            boolean isCreate);
}
