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
import com.unboundid.ldap.sdk.DN;
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
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.unboundid.UBIDLogger.LdapOp;

abstract class UBIDLdapOperation {
    private static Log debugLogger = ZimbraLog.ldap;
    
    protected UBIDLdapContext ctx;
    protected LDAPResponse resp;
    protected Timer timer;
    
    static GetConnection GET_CONNECTION(UBIDLdapContext ctx) {
        return new GetConnection(ctx);
    }
    
    static CreateEntry CREATE_ENTRY(UBIDLdapContext ctx) {
        return new CreateEntry(ctx);
    }
    
    static DeleteEntry DELETE_ENTRY(UBIDLdapContext ctx) {
        return new DeleteEntry(ctx);
    }
    
    static Search SEARCH(UBIDLdapContext ctx) {
        return new Search(ctx);
    }
    
    static GetEntry GET_ENTRY(UBIDLdapContext ctx) {
        return new GetEntry(ctx);
    }
    
    static GetSchema GET_SCHEMA(UBIDLdapContext ctx) {
        return new GetSchema(ctx);
    }
    
    static ModifyAttrs MODIFY_ATTRS(UBIDLdapContext ctx) {
        return new ModifyAttrs(ctx);
    }
    
    static TestAndModifyAttrs TEST_AND_MODIFY_ATTRS(UBIDLdapContext ctx) {
        return new TestAndModifyAttrs(ctx);
    }
    
    static ModifyDN MODIFY_DN(UBIDLdapContext ctx) {
        return new ModifyDN(ctx);
    }
    
    static GenericOp GENERIC_OP(LdapOp op, LdapUsage usage) {
        return new GenericOp(op, usage);
    }
    
    protected UBIDLdapOperation(UBIDLdapContext ctx) {
        this.ctx = ctx;
    }
    
    protected abstract LdapOp getOp();
    
    protected boolean debugEnabled() {
        return debugLogger.isDebugEnabled();
    }
    
    protected void beginOp() {
        if (!debugLogger.isDebugEnabled()) {
            return;
        }
        timer = new Timer();
        timer.start();
    }
    
    protected void endOp() {
        endOp(ctx.getConn(), null);
    }
    
    protected void endOp(String extraInfo) {
        endOp(ctx.getConn(), extraInfo);
    }
    
    protected void endOp(LDAPConnection conn, String extraInfo) {
        if (!debugLogger.isDebugEnabled()) {
            return;
        }
        
        debugLogger.debug(
                "%s - millis=[%d], resp=[%s], usage=[%s], conn=[%d]%s", 
                getOp(), 
                timer.elapsedMillis(), 
                getRespText(),
                getUsage().name(),
                conn.getConnectionID(), 
                extraInfo == null ? "" : ", " + extraInfo);
    }
    
    // only for GenericOp
    protected void endOp(LDAPResponse resp, String extraInfo) {
        if (!debugLogger.isDebugEnabled()) {
            return;
        }
        
        debugLogger.debug(
                "%s - millis=[%d], resp=[%s], usage=[%s]%s", 
                getOp(), 
                timer.elapsedMillis(), 
                getRespText(),
                getUsage().name(),
                extraInfo == null ? "" : ", " + extraInfo);
    }
    
    protected LdapUsage getUsage() {
        return ctx.getUsage();
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
    
    protected String getRespText() {
        if (resp == null) {
            return "null";
        } else if (resp instanceof LDAPResult) {
            return ((LDAPResult) resp).getResultCode().toString();
        } else {
            return resp.toString();
        }
    }
    
    private static class Timer {
        private long startTime;
        
        private void start() {
            startTime = System.currentTimeMillis();
        }
        
        private long elapsedMillis() {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * GetConnection
     */
    static class GetConnection extends UBIDLdapOperation {
        private GetConnection(UBIDLdapContext ctx) {
            super(ctx);
        }
                
        @Override
        protected LdapOp getOp() {
            return LdapOp.GET_CONN;
        }
        
        LDAPConnection execute(LDAPConnectionPool pool) throws LDAPException {
            LDAPConnection connection = null;
            
            beginOp();
            try {
                connection = pool.getConnection();
                return connection;
            } finally {
                if (debugEnabled()) {
                    endOp(connection, String.format("connPool=[%s(%d)]",
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

        private CreateEntry(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.CREATE_ENTRY;
        }
        
        LDAPResult execute(Entry entry) throws LDAPException {
            beginOp();
            try {
                resp = ctx.getConn().add(entry);
                return (LDAPResult) resp;
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("entry=[%s]", entry.toString()));
                }
            }
        }
    }
    
    
    /**
     * DeleteEntry
     */
    static class DeleteEntry extends UBIDLdapOperation {

        private DeleteEntry(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.DELETE_ENTRY;
        }
        
        LDAPResult execute(String dn) throws LDAPException {
            beginOp();
            try {
                resp = ctx.getConn().delete(dn);
                return (LDAPResult) resp;
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("dn=[%s]", dn));
                }
            }
        }
    }
    
    /**
     * Search
     */
    static class Search extends UBIDLdapOperation {

        private Search(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.SEARCH;
        }
        
        SearchResult execute(SearchRequest searchRequest) throws LDAPException {
            beginOp();
            try {
                resp = ctx.getConn().search(searchRequest);
                return (SearchResult) resp;
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("base=[%s], filter=[%s]",
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

        private GetEntry(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.GET_ENTRY;
        }
        
        SearchResultEntry execute(String dn, String[] attrs) throws LDAPException {
            beginOp();
            try {
                return (attrs == null) ? ctx.getConn().getEntry(dn) : ctx.getConn().getEntry(dn, attrs);
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("dn=[%s]", dn));
                }
            }
        }
    }
    
    
    /**
     * GetSchema
     */
    static class GetSchema extends UBIDLdapOperation {

        private GetSchema(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.GET_SCHEMA;
        }
        
        Schema execute() throws LDAPException {
            beginOp();
            try {
                return ctx.getConn().getSchema();
            } finally {
                if (debugEnabled()) {
                    endOp();
                }
            }
        }
    }
    
    
    /**
     * ModifyAttrs
     */
    static class ModifyAttrs extends UBIDLdapOperation {

        private ModifyAttrs(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.MODIFY_ATTRS;
        }
        
        LDAPResult execute(String dn, List<Modification> modList) throws LDAPException {
            beginOp();
            try {
                resp = ctx.getConn().modify(dn, modList);
                return (LDAPResult) resp;
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("dn=[%s], mod=[%s]", dn, getModListText(modList)));
                }
            }
        }
    }
    
    
    /**
     * TestAndModifyAttrs
     */
    static class TestAndModifyAttrs extends UBIDLdapOperation {

        private TestAndModifyAttrs(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.TEST_AND_MODIFY_ATTRS;
        }
        
        LDAPResult execute(ModifyRequest modReq) 
        throws LDAPException {
            beginOp();
            try {
                resp = ctx.getConn().modify(modReq);
                return (LDAPResult) resp;
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("dn=[%s], mod=[%s] control=[%s]", 
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

        private ModifyDN(UBIDLdapContext ctx) {
            super(ctx);
        }

        @Override
        protected LdapOp getOp() {
            return LdapOp.MODIFY_DN;
        }
        
        LDAPResult execute(String dn, String newRDN, boolean deleteOldRDN, String newSuperiorDN) 
        throws LDAPException {
            beginOp();
            try {
                resp = ctx.getConn().modifyDN(dn, newRDN, deleteOldRDN, newSuperiorDN);
                return (LDAPResult) resp;
            } finally {
                if (debugEnabled()) {
                    endOp(String.format("dn=[%s] newRDN=[%s] newSuperiorDN=[%s]", 
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
        private LdapOp op;
        private LdapUsage usage;
        
        private GenericOp(LdapOp op, LdapUsage usage) {
            super(null);
            this.op = op;
            this.usage = usage;
        }

        @Override
        protected LdapOp getOp() {
            return op;
        }
        
        @Override
        protected LdapUsage getUsage() {
            return usage;
        }
        
        void begin() {
            beginOp();
        }
        
        void end(LDAPResponse resp, String extraInfo) {
            if (debugEnabled()) {
                this.resp = resp;
                endOp(resp, extraInfo);
            }
        }
    }
}
