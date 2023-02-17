/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016, 2023 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import static com.zimbra.cs.filter.JsieveConfigMapHandler.CAPABILITY_EDITHEADER;
import static com.zimbra.cs.filter.jsieve.ComparatorName.ASCII_NUMERIC_COMPARATOR;

import java.util.List;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.exception.OperationException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter.PARSESTATUS;
import com.zimbra.cs.mime.Mime;

public class DeleteHeader extends AbstractCommand {
    private EditHeaderExtension ehe = new EditHeaderExtension();

    /** (non-Javadoc)
     * @see org.apache.jsieve.commands.AbstractCommand#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments, org.apache.jsieve.Block, org.apache.jsieve.SieveContext)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object executeBasic(MailAdapter mail, Arguments args, Block block, SieveContext sieveContext)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            ZimbraLog.filter.info("deleteheader: Zimbra mail adapter not found.");
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        if (ASCII_NUMERIC_COMPARATOR.equalsIgnoreCase(ehe.getComparator())) {
            Require.checkCapability((ZimbraMailAdapter) mail, ASCII_NUMERIC_COMPARATOR);
        }
        Require.checkCapability(mailAdapter, CAPABILITY_EDITHEADER);
        if (!mailAdapter.getAccount().isSieveEditHeaderEnabled()) {
            mailAdapter.setDeleteHeaderPresent(true);
            return null;
        }
        // make sure zcs do not delete immutable header
        if (EditHeaderExtension.isImmutableHeaderKey(ehe.getKey(), mailAdapter)) {
            ZimbraLog.filter.info("deleteheader: %s is immutable header, so exiting silently.", ehe.getKey());
            return null;
        }
        if (mailAdapter.getEditHeaderParseStatus() == PARSESTATUS.MIMEMALFORMED) {
            ZimbraLog.filter.debug("deleteheader: Triggering message is malformed MIME");
            return null;
        }

        if(mailAdapter.cloneParsedMessage()) {
            ZimbraLog.filter.debug("deleteHeader: failed to clone parsed message, so exiting silently.");
            return null;
        }

        // replace sieve variables
        ehe.replaceVariablesInValueList(mailAdapter);
        ehe.replaceVariablesInKey(mailAdapter);
        FilterUtil.headerNameHasSpace(ehe.getKey());

        MimeMessage mm = mailAdapter.getMimeMessage();
        Enumeration<Header> headers;
        try {
            headers = mm.getAllHeaders();
            if (!headers.hasMoreElements()) {
                ZimbraLog.filter.info("deleteheader: No headers found in mime.");
                return null;
            }
        } catch (MessagingException e) {
            throw new OperationException("deleteheader: Error occured while fetching all headers from mime.", e);
        }

        List <String> matchingeHeaderList = ehe.getMatchingHeaders(mm);
        int headerCount = matchingeHeaderList.size();
        if (headerCount < 1) {
            ZimbraLog.filter.info("deleteheader: No headers found matching with \"%s\" in mime.", ehe.getKey());
            return null;
        }
        ehe.setEffectiveIndex(headerCount);
        int matchIndex = 0;
        Set<String> removedHeaders = new HashSet<String>();

        try {
            boolean hasEdited = false;
            while (headers.hasMoreElements()) {
                Header header = headers.nextElement();
                boolean deleteCurrentHeader = false;
                if (header.getName().equalsIgnoreCase(ehe.getKey())){
                    matchIndex++;
                    if (ehe.getIndex() == null || (ehe.getIndex() != null && ehe.getIndex() == matchIndex)) {
                        if (ehe.getValueList() == null || ehe.getValueList().isEmpty()) {
                            deleteCurrentHeader = true;
                        } else {
                            for (String value : ehe.getValueList()) {
                                deleteCurrentHeader = ehe.matchCondition(mailAdapter, header, matchingeHeaderList, value, sieveContext);
                                if (deleteCurrentHeader) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!(removedHeaders.contains(header.getName()))) {
                    mm.removeHeader(header.getName());
                    removedHeaders.add(header.getName());
                    hasEdited = true;
                }
                // if deleteCurrentHeader is true, don't add header to mime
                if (!deleteCurrentHeader) {
                    mm.addHeaderLine(header.getName() + ": " + header.getValue());
                    hasEdited = true;
                } else {
                    if (LC.lmtp_extended_logs_enabled.booleanValue()) {
                        ZimbraLog.filter.info(
                                "deleteheader: name=%s value=%s", header.getName(), header.getValue()
                                + FilterUtil.getExtendedInfo(mm));
                    } else {
                        ZimbraLog.filter.info(
                                "deleteheader: deleted header in mime with name: %s and value: %s",
                                header.getName(), header.getValue());
                    }
                }
            }
            if (hasEdited) {
                EditHeaderExtension.saveChanges(mailAdapter, "deleteheader", mm);
                mailAdapter.updateIncomingBlob();
            }
        } catch (MessagingException me) {
            throw new OperationException("deleteheader: Error occured while operating mime.", me);
        }
        return null;
    }

    /** (non-Javadoc)
     * @see org.apache.jsieve.commands.AbstractCommand#validateArguments(org.apache.jsieve.Arguments, org.apache.jsieve.SieveContext)
     */
    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
        ZimbraLog.filter.debug("deleteheader: " + arguments.getArgumentList().toString());
        ehe.setupEditHeaderData(arguments, this);
        // Key must be present
        if (ehe.getKey() == null) {
            throw new SyntaxException("deleteheader: key not found.");
        }
        ehe.commonValidation("DeleteHeader");
    }
}