package com.zimbra.cs.filter.jsieve;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.comparators.ComparatorUtils;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SievePatternException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.SieveMailException;
import org.apache.jsieve.tests.Header;

import com.zimbra.cs.filter.ZimbraMailAdapter;

public class VariableHeader extends Header {
	protected boolean match(MailAdapter mail, String comparator, String matchType, List headerNames, List keys,
			SieveContext context) throws SieveException {
		if (!(mail instanceof ZimbraMailAdapter)) {
			return false;
		}
		
		ZimbraMailAdapter zma  = (ZimbraMailAdapter) mail;
		if (matchType.equals(MatchTypeTags.MATCHES_TAG)) {
			this.evaluateVarExp(zma, headerNames, keys);
		}
		List<String> newKeys = new ArrayList<String>();
		for (Object key : keys) {
			String keyT = (String) key;
			if (keyT.startsWith("$")) {
				keyT = zma.getVariable(keyT.substring(2, keyT.indexOf("}")));
				newKeys.add(keyT);
			} else {
				newKeys.add(keyT);
			}
		}

		// Iterate over the header names looking for a match
		boolean isMatched = false;
		Iterator<String> headerNamesIter = headerNames.iterator();
		while (!isMatched && headerNamesIter.hasNext()) {
			Set<String> values = zma.getMatchingHeaderFromAllParts(headerNamesIter.next());
			isMatched = match(comparator, matchType, new ArrayList<String>(values), newKeys, context);
		}
		return isMatched;
	}
	
	private void evaluateVarExp(ZimbraMailAdapter mailAdapter, List headerNames, List keys) throws SieveMailException, SievePatternException {
		StringBuilder sb = new StringBuilder();
		for (Object obj : keys) {
			sb.append((String)obj);
		}
		String regEx = sb.toString();
		for (Object headerName : headerNames) {
			String hn = (String) headerName;
			List<String> values = mailAdapter.getMatchingHeader(hn);
			sb = new StringBuilder();
			for (String value : values) {
				sb.append(value);
			}
			
		}
		String matchString  = sb.toString();
		List<String> varValues = new ArrayList<String>();
		try {
			String regex = ComparatorUtils.sieveToJavaRegex(regEx);
			Matcher matcher = Pattern.compile(regex).matcher(matchString);
			while (matcher.find()) {
				String matchGrp =  matcher.group();
				if (!StringUtils.isEmpty(matchGrp.trim())) {
					varValues.add(matchGrp);
				}
			}
		} catch (PatternSyntaxException e) {
			throw new SievePatternException(e.getMessage());
		}
		mailAdapter.setMatchedValues(varValues);
	}

}
