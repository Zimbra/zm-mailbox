/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.commands.extensions.Log;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author zimbra
 *
 */
public class VariableLog extends Log {
	private Map<String, String> variables = new HashMap<String, String>();
	private List<String> matchedValues = new ArrayList<String>();

	@Override 
	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block,
			SieveContext context) throws SieveException {

		if (!(mail instanceof ZimbraMailAdapter)) {
			return null;
		}

		ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
		if (SetVariable.isVariablesExtAvailable(mailAdapter)) {
			this.variables = mailAdapter.getVariables();
			this.matchedValues = mailAdapter.getMatchedValues();
		}
		return super.executeBasic(mail, arguments, block, context);

	}

	@Override
	protected void log(String logLevel, String message, SieveContext context) throws SyntaxException {
		message = FilterUtil.replaceVariables(variables, matchedValues, message);
		super.log(logLevel, message, context);
	}

}
