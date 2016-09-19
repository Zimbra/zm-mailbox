/**
 * 
 */
package com.zimbra.cs.filter.jsieve;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author rdesai
 *
 */
public class SetVariable extends AbstractCommand {

	public static final String ALL_LOWER_CASE = "lower";
	public static final String ALL_UPPER_CASE = "upper";
	public static final String LOWERCASE_FIRST = "lowerfirst";
	public static final String UPPERCASE_FIRST = "upperfirst";
	public static final String QUOTE_WILDCARD = "quotewildcard";
	public static final String STRING_LENGTH = "length";
	
	@Override
	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
			throws SieveException {
		List<Argument> args = arguments.getArgumentList();
		this.validateArguments(arguments, context);

		if (!(mail instanceof ZimbraMailAdapter)) {
			return null;
		}
		ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
		Map<String, String> existingVars = mailAdapter.getVariables();
		List<String> matchedValues = mailAdapter.getMatchedValues();
		String [] operations = new String[6];
		String key = null;
		String value = null;
		int index = 0;
		for (Argument a: arguments.getArgumentList()) {
			if (a instanceof TagArgument) {
				TagArgument tag = (TagArgument) a;
				String tagValue = tag.getTag();
				if (tagValue.startsWith(":") && isValidModifier(tagValue)) {
					operations[getIndex(tagValue)] = tagValue.substring(1);
				}			
			} else {
				String argument = ((StringListArgument) a).getList().get(0);
				if (argument.startsWith(":")) {
					operations[getIndex(argument)] = argument.substring(1);
				} else {
					if (index == 0) {
						key = argument;
					} else {
						if (argument.startsWith("${")) {
							value = FilterUtil.replaceVariables(existingVars, matchedValues, argument);
						} else {
							value = argument;
						}
					}
					++index;
				}
			}
		}
		value = applyModifiers(value, operations);
		mailAdapter.addVariable(key, value);
		
//		if (value.startsWith("${")) {
//			value = FilterUtil.replaceVariables(mailAdapter.getVariables(), mailAdapter.getMatchedValues(), value);
//		}
//		mailAdapter.addVariable(key, value);
		return null;
	}

	/**
	 * @param argument
	 * @return
	 */
	public static int getIndex(String operation) {
		int index = 0;
		operation = operation.substring(1);
		if (operation.equals(STRING_LENGTH)) {
			index = 5;
		} else if (operation.equals(QUOTE_WILDCARD)) {
			index = 4;
		} else if (operation.equals(LOWERCASE_FIRST)) {
			index = 3;
		} else if (operation.equals(UPPERCASE_FIRST)) {
			index = 2;
		} else if (operation.equals(ALL_LOWER_CASE)) {
			index = 1;
		} else if (operation.equals(ALL_UPPER_CASE)) {
			index =0;
		} 
		
		return index;
	}

	/**
	 * @param value
	 * @param operations
	 * @return
	 */
	public static String applyModifiers(String value, String[] operations) {
		String temp = value;
		for (int i = 0; i < operations.length; ++i) {
			String operation = operations[i];
			if (operation == null) {
				continue;
			}
			if (operation.equals(STRING_LENGTH)) {
				temp = String.valueOf(value.length());
			} else if (operation.equals(QUOTE_WILDCARD)) {
				temp = temp.replaceAll("\\\\", "\\\\\\\\");
				temp = temp.replaceAll("\\*", "\\\\*");
				temp = temp.replaceAll("\\?", "\\\\?");	
			} else if (operation.equals(LOWERCASE_FIRST)) {
				temp = String.valueOf(temp.charAt(0)).toLowerCase() + temp.substring(1);
			} else if (operation.equals(UPPERCASE_FIRST)) {
				temp = String.valueOf(temp.charAt(0)).toUpperCase() + temp.substring(1);
			} else if (operation.equals(ALL_LOWER_CASE)) {
				temp = value.toLowerCase();
			} else if (operation.equals(ALL_UPPER_CASE)) {
				temp = temp.toUpperCase();
			} else {
				temp = value;
			}
		}
		return temp;
	}

	@Override
	protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
		List<Argument> args = arguments.getArgumentList();
//		":lower" / ":upper" / ":lowerfirst" / ":upperfirst" /
//        ":quotewildcard" / ":length"
//		  set "name" "Ethelbert"
//		  set "a" "juMBlEd lETteRS";             => "juMBlEd lETteRS"
//	      set :length "b" "${a}";                => "15"
//	      set :lower "b" "${a}";                 => "jumbled letters"
//	      set :upperfirst "b" "${a}";            => "JuMBlEd lETteRS"
//	      set :upperfirst :lower "b" "${a}";     => "Jumbled letters"
//	      set :quotewildcard "b" "Rock*";        => "Rock\*"
		int varArgCount = 0;
		String key = null;
		if (args.size() >= 2) {
			for (Argument a: arguments.getArgumentList()) {
				
				if (a instanceof TagArgument) {
					TagArgument tag = (TagArgument) a;
					String tagValue = tag.getTag();
					if (tagValue.startsWith(":") && !isValidModifier(tagValue)) {
						throw new SyntaxException("Invalid variable modifier:" + tagValue);
					}			
				} else {
					String argument = ((StringListArgument)a).getList().get(0);
					if (argument.startsWith(":") && !isValidModifier(argument)) {
						throw new SyntaxException("Invalid variable modifier:" + argument);
					} else {
						ZimbraLog.filter.debug("set variable argument: " + argument);
						if (varArgCount == 0) {
							key = argument;
						}
						++ varArgCount;
					}
				}
			}
			if (varArgCount != 2) {
				throw new SyntaxException("Exactly 2 arguments permitted. Found " + varArgCount);
			} else {
				if (!(isValidIdentifier(key))) {
					
				}
			}
		}  else {
			throw new SyntaxException("Atleast 2 argument are needed. Found " + arguments);
		}
	}

	/**
	 * @param key
	 * @return
	 */
	private boolean isValidIdentifier(String key) {
		Pattern pattern = Pattern.compile("([a-zA-Z_])*",  Pattern.CASE_INSENSITIVE);
        if (pattern.matcher(key).matches()) {
           return true;
        }
    	return false;
	}

	/**
	 * @param argument
	 * @return
	 */
	private boolean isValidModifier(String modifier) {
		String temp = modifier.substring(1);
		temp = temp.toLowerCase();
		ZimbraLog.filter.debug("Set variable modifier is:" + temp);
		if (temp.equals(ALL_LOWER_CASE) || temp.equals(ALL_UPPER_CASE) || temp.equals(LOWERCASE_FIRST) 
				|| temp.equals(UPPERCASE_FIRST) || temp.equals(QUOTE_WILDCARD) || temp.equals(STRING_LENGTH)) {
			return true;
		}
		return false;
	}
	
	
	

}
