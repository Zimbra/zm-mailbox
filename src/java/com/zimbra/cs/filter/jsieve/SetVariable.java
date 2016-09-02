/**
 * 
 */
package com.zimbra.cs.filter.jsieve;

import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author rdesai
 *
 */
public class SetVariable extends AbstractCommand {

	@Override
	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
			throws SieveException {
		List<Argument> args = arguments.getArgumentList();
		if (args.size() != 2) {
			throw new SyntaxException("Exactly 2 argument permitted. Found " + args.size());
		}

		if (!(mail instanceof ZimbraMailAdapter)) {
			return null;
		}
		StringListArgument sta = (StringListArgument) args.get(0);
		String key = sta.getList().get(0);
		sta = (StringListArgument) args.get(1);
		String value = sta.getList().get(0);
		ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
		if (value.startsWith("${")) {
			value = FilterUtil.replaceVariables(mailAdapter.getVariables(), mailAdapter.getMatchedValues(), value);
		}
		mailAdapter.addVariable(key, value);
		mail.addAction(new ActionVariableExp(key, value));
		return null;
	}

	@Override
	protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
		List<Argument> args = arguments.getArgumentList();
		if (args.size() != 2) {
			throw new SyntaxException("Exactly 2 argument permitted. Found " + args.size());
		}

	}

}
