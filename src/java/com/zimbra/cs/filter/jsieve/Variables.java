package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

public class Variables extends AbstractActionCommand {

	@Override
	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
			throws SieveException {

		return null;
	}

}
