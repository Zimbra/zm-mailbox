/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap.unboundid;

import java.util.List;

import com.unboundid.ldap.protocol.LDAPResponse;
import com.unboundid.ldap.sdk.CompareRequest;
import com.unboundid.ldap.sdk.CompareResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ExtendedRequest;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedRequest;
import com.unboundid.ldap.sdk.schema.Schema;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ldap.LdapOp;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.stats.ZimbraPerf;

abstract class UBIDLdapOperation {
    private static final boolean STATS_ENABLED = true;

    private static Log debugLogger = ZimbraLog.ldap;

    static final GetConnection GET_CONNECTION = new GetConnection();
    static final CreateEntry CREATE_ENTRY = new CreateEntry();
    static final DeleteEntry DELETE_ENTRY = new DeleteEntry();
    static final Search SEARCH = new Search();
    static final Compare COMPARE = new Compare();
    static final GetEntry GET_ENTRY = new GetEntry();
    static final GetSchema GET_SCHEMA = new GetSchema();
    static final ModifyAttrs MODIFY_ATTRS = new ModifyAttrs();
    static final TestAndModifyAttrs TEST_AND_MODIFY_ATTRS = new TestAndModifyAttrs();
    static final ModifyDN MODIFY_DN = new ModifyDN();
    static final SetPassword SET_PASSWORD = new SetPassword();
    static final GenericOp GENERIC_OP = new GenericOp();

    protected void stat(long startTime) {
        stat(startTime, getOp());
    }

    protected void searchStat(long startTime, String statString) {
        if (STATS_ENABLED) {
            ZimbraPerf.LDAP_TRACKER.addStat(statString, startTime);
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
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().add(entry);
                    stat(startTime);
                    return result;
                } else {
                    throw e;
                }
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
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().delete(dn);
                    stat(startTime);
                    return result;
                } else {
                    throw e;
                }
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

        SearchResult execute(UBIDLdapContext ctx, SearchRequest searchRequest, ZLdapFilter zFilter)
        throws LDAPException {
            SearchResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().search(searchRequest);
                searchStat(startTime, zFilter.getStatString());
                return result;
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().search(searchRequest);
                    searchStat(startTime, zFilter.getStatString());
                    return result;
                } else {
                    throw e;
                }
            } finally {
                if (debugEnabled()) {
                    Control[] controls = searchRequest.getControls();
                    StringBuffer ctls = new StringBuffer();
                    if (controls != null) {
                        boolean first = true;
                        for (Control control : controls) {
                            if (first) {
                                first = false;
                            } else {
                                ctls.append(",");
                            }
                            ctls.append(control.getControlName());
                        }
                    }
                    String extraInfo = ctls.length() == 0 ?
                            String.format("base=[%s], filter=[%s]",
                                    searchRequest.getBaseDN(),
                                    searchRequest.getFilter().toString()) :
                            String.format("controls=[%s], base=[%s], filter=[%s]",
                                    ctls,
                                    searchRequest.getBaseDN(),
                                    searchRequest.getFilter().toString());

                    debug(ctx,  startTime, result, extraInfo);
                }
            }
        }
    }

    /**
     * Compare
     */
    static class Compare extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.COMPARE;
        }

        CompareResult execute(UBIDLdapContext ctx, CompareRequest compareRequest)
        throws LDAPException {
            CompareResult result = null;
            long startTime = System.currentTimeMillis();
            try {
                result = ctx.getConn().compare(compareRequest);
                stat(startTime);
                return result;
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().compare(compareRequest);
                    stat(startTime);
                    return result;
                } else {
                    throw e;
                }
            } finally {
                if (debugEnabled()) {
                    Control[] controls = compareRequest.getControls();
                    StringBuffer ctls = new StringBuffer();
                    if (controls != null) {
                        boolean first = true;
                        for (Control control : controls) {
                            if (first) {
                                first = false;
                            } else {
                                ctls.append(",");
                            }
                            ctls.append(control.getControlName());
                        }
                    }
                    compareRequest.getAssertionValue();
                    String extraInfo =
                            String.format("[%sDN=[%s], attribute=[%s] assertionValue=[%s]",
                                    ctls.length() == 0 ? "" : String.format("controls=[%s], ", ctls),
                                    compareRequest.getDN(), compareRequest.getAttributeName(),
                                    compareRequest.getAssertionValue());

                    debug(ctx,  startTime, result, extraInfo);
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
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    SearchResultEntry entry = (attrs == null) ? ctx.getConnectionPool().getEntry(dn) :
                            ctx.getConnectionPool().getEntry(dn, attrs);
                    stat(startTime);
                    return entry;
                } else {
                    throw e;
                }
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
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    Schema schema = ctx.getConnectionPool().getSchema();
                    stat(startTime);
                    return schema;
                } else {
                    throw e;
                }
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
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().modify(dn, modList);
                    stat(startTime);
                    return result;
                } else {
                    throw e;
                }
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
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().modifyDN(dn, newRDN, deleteOldRDN, newSuperiorDN);
                    stat(startTime);
                    return result;
                } else {
                    throw e;
                }
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("dn=[%s] newRDN=[%s] newSuperiorDN=[%s]",
                            dn, newRDN, newSuperiorDN));
                }
            }
        }
    }

    static class SetPassword extends UBIDLdapOperation {

        @Override
        protected LdapOp getOp() {
            return LdapOp.SET_PASSWORD;
        }

        LDAPResult execute(UBIDLdapContext ctx, String dn, String newPassword) throws LDAPException {
            LDAPResult result = null;
            long startTime = System.currentTimeMillis();
            ExtendedRequest req = new PasswordModifyExtendedRequest(dn, null, newPassword);
            try {
                result = ctx.getConn().processExtendedOperation(req);
                stat(startTime);
                return result;
            } catch (LDAPException e) {
                if (ResultCode.SERVER_DOWN == e.getResultCode()) {
                    result = ctx.getConnectionPool().processExtendedOperation(req);
                    stat(startTime);
                    return result;
                } else {
                    throw e;
                }
            } finally {
                if (debugEnabled()) {
                    debug(ctx, startTime, result,
                            String.format("dn=[%s]", dn));
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
