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
import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class Redirect extends org.apache.jsieve.commands.Redirect {

	@Override
	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
			throws SieveException {
		
		 if (!(mail instanceof ZimbraMailAdapter))
	            return null;
	    	List<Argument> args = arguments.getArgumentList();
			if (args.size() == 1) {
				// default redirect behavior if :copy argument is absent
				return super.executeBasic(mail, arguments, block, context);
			} else {
				// save a copy to inbox
				try {
					FilterUtil.copyToInbox(mail);
				} catch (ServiceException e) {
					throw new SieveException("Failed to save copy to inbox");
				}
				// remove :copy argument from arguments e.g. redirect :copy "test3@zimbra.com" => redirect "test3@zimbra.com"
				List<Argument> fieldArgumentList = new ArrayList<Argument>();
				fieldArgumentList.add(args.get(1));
				Arguments newArguments = new Arguments(fieldArgumentList, null);
				
				// default redirect behavior for specified folder argument => redirect "test3@zimbra.com"
				return super.executeBasic(mail, newArguments, block, context);
			}
	}

	@Override
	protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
		List<Argument> args = arguments.getArgumentList();
	    if (args.size() < 1 || args.size() > 2) {
	      throw context.getCoordinate().syntaxException("Exactly 1 or 2 arguments permitted. Found " + args.size());
	    }
	    
	    Argument argument;
	    String copyArg;
	    if(args.size() == 1) {
	    	// address list argument
	    	argument = (Argument)args.get(0);
	    } else {
	    	copyArg = ((Argument)args.get(0)).getValue().toString();
	    	// if arguments size is 2; first argument should be :copy
	    	if (!copyArg.equals(":copy")) {
	  	      throw new SyntaxException("Expecting :copy");
	  	    } 
	    	// address list argument
	    	argument = (Argument)args.get(1);
	    }
	    // address list argument should be a String list
	    if (!(argument instanceof StringListArgument)) {
	      throw new SyntaxException("Expecting a string-list");
	    } 
	    // address list argument should contain exactly one address  
	    if (1 != ((StringListArgument)argument).getList().size()) {
	      throw new SyntaxException("Expecting exactly one argument");
	    }
	}

}
