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
package com.zimbra.cs.ldap.unboundid;

import java.util.List;

import com.unboundid.ldap.protocol.LDAPResponse;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.schema.Schema;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.unboundid.UBIDLogger.LdapOp;
import com.zimbra.cs.stats.ZimbraPerf;

abstract class UBIDLdapOperation {
    private static final boolean STATS_ENABLED = true;
    
    private static Log debugLogger = ZimbraLog.ldap;
    
    static final GetConnection GET_CONNECTION = new GetConnection();
    static final CreateEntry CREATE_ENTRY = new CreateEntry();
    static final DeleteEntry DELETE_ENTRY = new DeleteEntry();
    static final Search SEARCH = new Search();
    static final GetEntry GET_ENTRY = new GetEntry();
    static final GetSchema GET_SCHEMA = new GetSchema();
    static final ModifyAttrs MODIFY_ATTRS = new ModifyAttrs();
    static final TestAndModifyAttrs TEST_AND_MODIFY_ATTRS = new TestAndModifyAttrs();
    static final ModifyDN MODIFY_DN = new ModifyDN();
    static final GenericOp GENERIC_OP = new GenericOp();
    
    protected void stat(long startTime) {
        stat(startTime, getOp());
    }
    
    protected void searchStat(long startTime, String param) {
        if (STATS_ENABLED) {
            ZimbraPerf.LDAP_TRACKER.addStat(getOp().name() + " " + param, startTime);
        }
    }
    
    protected void stat(long startTime, LdapOp op) {
        stat(startTime, op.name());
    }
    
    private void stat(long startTime, String op) {
        if (STATS_ENABLED) {
            ZimbraPerf.LDAP_TRACKER.addStat(op, startTime);
        }
    }
    
    protected abstract LdapOp getOp();

    protected boolean debugEnabled() {
        return debugLogger.isDebugEnabled();
    }
    
    protected void debug(UBIDLdapContext ctx, long startTime) {
        debug(ctx, startTime, null);
    }
    
    protected void debug(UBIDLdapContext ctx, long startTime, String extraInfo) {
        debug(ctx, startTime, ctx.getConn(), extraInfo);
    }
    
    // for ops not returning a LDAPResponse
    protected void debug(UBIDLdapContext ctx, long startTime, 
            LDAPConnection conn, String extraInfo) {
        debugLogger.debug(
                "%s - millis=[%d], usage=[%s], conn=[%s]%s", 
                getOp().name(), 
                System.currentTimeMillis() - startTime, 
                ctx.getUsage().name(),
                conn == null ? "" : conn.getConnectionID(), 
                extraInfo == null ? "" : ", " + extraInfo);
    }
    
    // for ops returning a LDAPResponse
    protected void debug(UBIDLdapContext ctx, long startTime, LDAPResponse resp,
            String extraInfo) {
        debugLogger.debug(
                "%s - millis=[%d], resp=[%s], usage=[%s], conn=[%s]%s", 
                getOp().name(), 
                System.currentTimeMillis() - startTime, 
                getRespText(resp),
                ctx.getUsage().name(),
                ctx.getConn().getConnectionID(), 
                extraInfo == null ? "" : ", " + extraInfo);
    }
    
    // only for GenericOp
    protected void debug(LdapOp op, LdapUsage usage, long startTime, LDAPResponse resp, 
            String extraInfo) {
        debugLogger.debug(
                "%s - millis=[%d], resp=[%s], usage=[%s]%s", 
                op.name(), 
                System.currentTimeMillis() - startTime, 
                getRespText(resp),
                usage.name(),
                extraInfo == null ? "" : ", " + extraInfo);
    }
    
    protected String getConnectionPoolLogName(LDAPConnectionPool connPool) {
        String name = connPool.getConnectionPoolName();
        if (LdapConnectionPool.CP_ZIMBRA_REPLICA.equals(name) ||
            LdapConnectionPool.CP_ZIMBRA_MASTER.equals(name)) {
            return name;
        } else {
            // hide the password
            return ExternalLdapConfig.ConnPoolKey.getDisplayName(name);
        }
    }
    
    protected String getModListText(List<Modification> modList) {
        StringBuilder buffer = new StringBuilder();
        for (Modification mod : modList) {
            buffer.append(mod.toString() + ", ");
        }
        return buffer.toString();
    }
    
    protected String getCtlListText(List<Control> ctlList) {
        StringBuilder buffer = new StringBuilder();
        for (Control ctl : ctlList) {
            buffer.append(ctl.toString() + ", ");
        }
        return buffer.toString();
    }
    
    protected String getRespText(LDAPResponse resp) {
        if (resp == null) {
            return "null";
        } else if (resp instanceof LDAPResult) {
            return ((LDAPResult) resp).getResultCode().toString();
        } else {
            return resp.toString();
        }
    }

    
    /**
     * GetConnection
     */
    static class GetConnection extends UBIDLdapOperation {
                
        @Override
        protected LdapOp getOp() {
            return LdapOp.GET_CONN;
        }
        
        LDAPConnection execute(UBIDLdapContext ctx, LDAPConnectionPool pool) 
        throws LDAPException {
            LDAPConnection connection = null;
            long startTime = System.currentTimeMillis();
            try {
                connection = pool.getConnection();
                stat(startTime);
                return connection;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, connection, 
                            String.format("connPool=[%s(%d)]",
                            getConnectionPoolLogName(pool), 
                            pool.hashCode()));
                }
            }
        }
    }
   
    /**
     * CreateEntry
     */
    static class CreateEntry extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.CREATE_ENTRY;
        }
        
        LDAPResult execute(UBIDLdapContext ctx, Entry entry) throws LDAPException {
            LDAPResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().add(entry);
                stat(startTime);
                return result;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("entry=[%s]", entry.toString()));
                }
            }
        }
    }
    
    
    /**
     * DeleteEntry
     */
    static class DeleteEntry extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.DELETE_ENTRY;
        }
        
        LDAPResult execute(UBIDLdapContext ctx, String dn) throws LDAPException {
            LDAPResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().delete(dn);
                stat(startTime);
                return result;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("dn=[%s]", dn));
                }
            }
        }
    }
    
    /**
     * Search
     */
    static class Search extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.SEARCH;
        }
        
        SearchResult execute(UBIDLdapContext ctx, SearchRequest searchRequest) 
        throws LDAPException {
            return execute(ctx, searchRequest, null);
        }
        
        SearchResult execute(UBIDLdapContext ctx, SearchRequest searchRequest, ZLdapFilter zFilter) 
        throws LDAPException {
            SearchResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().search(searchRequest);
                if (zFilter == null) {
                    stat(startTime);
                } else {
                    searchStat(startTime, zFilter.getStatString());
                }
                return result;
            } finally {
                if (debugEnabled()) {
                    debug(ctx,  startTime, result,
                            String.format("base=[%s], filter=[%s]",
                            searchRequest.getBaseDN(),
                            searchRequest.getFilter().toString()));
                }
            }
        }
    }
    
    
    /**
     * GetEntry
     */
    static class GetEntry extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.GET_ENTRY;
        }
        
        SearchResultEntry execute(UBIDLdapContext ctx, String dn, String[] attrs) 
        throws LDAPException {
            long startTime = System.currentTimeMillis();
            try {
                SearchResultEntry entry = (attrs == null) ? ctx.getConn().getEntry(dn) : 
                    ctx.getConn().getEntry(dn, attrs);
                stat(startTime);
                return entry;
            } finally {
                if (debugEnabled()) {
                    debug(ctx,  startTime, 
                            String.format("dn=[%s]", dn));
                }
            }
        }
    }
    
    
    /**
     * GetSchema
     */
    static class GetSchema extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.GET_SCHEMA;
        }
        
        Schema execute(UBIDLdapContext ctx) throws LDAPException {
            long startTime = System.currentTimeMillis();
            try {
                Schema schema = ctx.getConn().getSchema();
                stat(startTime);
                return schema;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime);
                }
            }
        }
    }
    
    
    /**
     * ModifyAttrs
     */
    static class ModifyAttrs extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.MODIFY_ATTRS;
        }
        
        LDAPResult execute(UBIDLdapContext ctx, String dn, List<Modification> modList) 
        throws LDAPException {
            LDAPResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().modify(dn, modList);
                stat(startTime);
                return result;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("dn=[%s], mod=[%s]", dn, getModListText(modList)));
                }
            }
        }
    }
    
    
    /**
     * TestAndModifyAttrs
     */
    static class TestAndModifyAttrs extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.TEST_AND_MODIFY_ATTRS;
        }
        
        LDAPResult execute(UBIDLdapContext ctx, ModifyRequest modReq) 
        throws LDAPException {
            LDAPResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().modify(modReq);
                stat(startTime);
                return result;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("dn=[%s], mod=[%s] control=[%s]", 
                            modReq.getDN(), 
                            getModListText(modReq.getModifications()),
                            getCtlListText(modReq.getControlList())));
                }
            }
        }
    }
    
    
    /**
     * ModifyDN
     */
    static class ModifyDN extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.MODIFY_DN;
        }
        
        LDAPResult execute(UBIDLdapContext ctx, String dn, String newRDN, 
                boolean deleteOldRDN, String newSuperiorDN) 
        throws LDAPException {
            LDAPResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().modifyDN(dn, newRDN, deleteOldRDN, newSuperiorDN);
                stat(startTime);
                return result;
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("dn=[%s] newRDN=[%s] newSuperiorDN=[%s]", 
                            dn, newRDN, newSuperiorDN));
                }
            }
        }
    }
    
    
    /**
     * GenericOp
     * 
     * A wrapper for generic, UBIDLdapContext-less, LDAP operation or a sequence of 
     * operations that need to be timed and logged.
     * 
     * Callsite is responsible for always calling begin() and end() around the 
     * LDAP operation(s).
     */
    static class GenericOp extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            assert(false);
            return null;
        }
        
        long begin() {
            return System.currentTimeMillis();
        }
        
        void end(LdapOp op, LdapUsage usage, long startTime, boolean needsStat, 
                LDAPResponse resp, String extraInfo) {
            if (needsStat) {
                stat(startTime, op);
            }
            if (debugEnabled()) {
                debug(op, usage, startTime, resp, extraInfo);
            }
        }
    }
}
