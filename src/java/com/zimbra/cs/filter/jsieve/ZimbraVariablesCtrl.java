package com.zimbra.cs.filter.jsieve;

import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class ZimbraVariablesCtrl extends AbstractActionCommand {
    static final String RESET = ":reset";
    static final String VARIABLE_OFF = ":off";
    static final String VARIABLE_ON  = ":on";

    @Override
    protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
            throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            ZimbraLog.filter.info("deleteheader: Zimbra mail adapter not found.");
            return null;
        }
        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;

        for (Argument arg: arguments.getArgumentList()) {
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                String tagValue = tag.getTag();
                if (RESET.equalsIgnoreCase(tagValue)) {
                    mailAdapter.clearValues();
                } else if (VARIABLE_OFF.equalsIgnoreCase(tagValue)) {
                    mailAdapter.setVariablesExtAvailable(ZimbraMailAdapter.VARIABLETYPE.OFF);
                } else if (VARIABLE_ON.equalsIgnoreCase(tagValue)) {
                    mailAdapter.setVariablesExtAvailable(ZimbraMailAdapter.VARIABLETYPE.AVAILABLE);
                }
            }
        }
        return null;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context)
    throws SieveException
    {
        List<Argument> args = arguments.getArgumentList();
        if (args.size() > 2) {
            throw new SyntaxException(
                "More than 2 arguments found (" + args.size() + ")");
        }

        for (Argument arg: arguments.getArgumentList()) {
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                String tagValue = tag.getTag();
                if (!RESET.equalsIgnoreCase(tagValue) &&
                    !VARIABLE_OFF.equalsIgnoreCase(tagValue) &&
                    !VARIABLE_ON.equalsIgnoreCase(tagValue)) {
                    throw new SyntaxException("Invalid tag: [" + tagValue + "]");
                }
            } else {
                if (arg instanceof StringListArgument) {
                    String argument = ((StringListArgument) arg).getList().get(0);
                    throw new SyntaxException("Invalid argument: [" + argument + "]");
                } else {
                    throw new SyntaxException("Invalid argument");
                }
            }
        }
    }
}
