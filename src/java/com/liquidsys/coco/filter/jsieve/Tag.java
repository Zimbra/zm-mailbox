/*
 * Created on Nov 1, 2004
 *
 */
package com.liquidsys.coco.filter.jsieve;

import java.util.List;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.SyntaxException;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.mail.MailAdapter;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Tag extends AbstractActionCommand {

    /* (non-Javadoc)
     * @see org.apache.jsieve.commands.AbstractCommand#executeBasic(org.apache.jsieve.mail.MailAdapter, org.apache.jsieve.Arguments, org.apache.jsieve.Block)
     */
    protected Object executeBasic(MailAdapter mail, Arguments args, Block block)
            throws SieveException {
        
        // TODO Auto-generated method stub
        String tagName =
            (String) ((StringListArgument) args.getArgumentList().get(0))
                .getList().get(0);

        // Only one tag with the same tag name allowed, others should be
        // discarded?            
        
        mail.addAction(new ActionTag(tagName));

        return null;
    }
    
    /**
     * @see org.apache.jsieve.commands.AbstractCommand#validateArguments(Arguments)
     */
    protected void validateArguments(Arguments arguments) throws SieveException
    {
        List args = arguments.getArgumentList();
        if (args.size() != 1)
            throw new SyntaxException(
                "Exactly 1 argument permitted. Found " + args.size());

        Object argument = args.get(0);
        if (!(argument instanceof StringListArgument))
            throw new SyntaxException("Expecting a string-list");

        if (1 != ((StringListArgument) argument).getList().size())
            throw new SyntaxException("Expecting exactly one argument");
    }
}
