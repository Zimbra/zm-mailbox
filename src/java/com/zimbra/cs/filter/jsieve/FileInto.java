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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

import java.util.ArrayList;
import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;


public class FileInto extends org.apache.jsieve.commands.optional.FileInto {
	
	private static final String copy = ":copy";

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context) throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter))
            return null;
    	List<Argument> args = arguments.getArgumentList();
		if (args.size() == 1) {
			// default fileinto behavior if :copy argument is absent
			return super.executeBasic(mail, arguments, block, context);
		} else {
			// save a copy to inbox
			try {
				FilterUtil.copyToInbox(mail);
			} catch (ServiceException e) {
				throw new SieveException("Failed to save copy to inbox");
			}
			
			// remove :copy argument from arguments e.g. fileinto :copy "Junk" => fileinto "Junk"
			List<Argument> fieldArgumentList = new ArrayList<Argument>();
			fieldArgumentList.add(args.get(1));
			Arguments newArguments = new Arguments(fieldArgumentList, null);
			
			// default fileinto behavior for specified folder argument => fileinto "Junk"
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
	    	// folder list argument
	    	argument = (Argument)args.get(0);
	    } else {
	    	copyArg = ((Argument)args.get(0)).getValue().toString();
	    	// if arguments size is 2; first argument should be :copy
	    	if (!copyArg.equals(copy)) {
	  	      throw context.getCoordinate().syntaxException("Error in sieve fileinto. Expecting argument :copy");
	  	    } 
	    	// folder list argument
	    	argument = (Argument)args.get(1);
	    }
	    // folder list argument should be a String list
	    if (!(argument instanceof StringListArgument)) {
	      throw context.getCoordinate().syntaxException("Expecting a string-list");
	    } 
	    // folder list argument should contain exactly one folder name  
	    if (1 != ((StringListArgument)argument).getList().size()) {
	      throw context.getCoordinate().syntaxException("Expecting exactly one argument");
	    }
	}

	
}
