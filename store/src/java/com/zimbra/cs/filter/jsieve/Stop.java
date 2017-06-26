package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * Class Stop implements the stop control as defined in RFC 5228, section 3.3.
 */
public class Stop extends org.apache.jsieve.commands.Stop {

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        mailAdapter.setStop(true);
        return super.executeBasic(mail, arguments, block, context);
    }

}
