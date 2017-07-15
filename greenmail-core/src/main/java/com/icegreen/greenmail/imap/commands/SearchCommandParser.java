/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.search.AndTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles processing for the SEARCH imap command.
 *
 * @author Eli Pony <elipony@outlook.com>
 */
class SearchCommandParser extends CommandParser {
    private final Logger log = LoggerFactory.getLogger(SearchCommandParser.class);
    private final String spaceCharReplacement = "_SPACE_CHAR_REPLACEMENT_";

    /**
     * Parses the request argument into a valid search term. Not yet fully implemented - see SearchKey enum.
     * <p>
     * Other searches will return everything for now.
     * </p>
     */
    public SearchTerm searchTerm(ImapRequestLineReader request) throws ProtocolException, CharacterCodingException {
        String searchRequestString = getFullSearchRequest(request);
        return getSearchTerms(searchRequestString);
    }

    private SearchTerm getSearchTerms(String searchRequestString) {
        SearchTerm resultTerm;
        Stack<Object> valuesStack = new Stack<>();
        SearchTermBuilder searchTermBuilder;
        int numOfSubsequentSubjectTerms = 0;
        int parsedCommandIndex = 0;

        searchRequestString = removeQuotesAndSpace(searchRequestString);
        searchRequestString = removeParenthesesIfPresent(searchRequestString);
        ArrayList<String> searchWords = stringToTermsList(searchRequestString);

        for (String item : searchWords) {
            try {
                SearchKey key = SearchKey.valueOf(item);

                if (key == SearchKey.SUBJECT) {
                    numOfSubsequentSubjectTerms++;
                }

                if (key == SearchKey.NOT) {
                    SearchTerm term = (SearchTerm) valuesStack.pop();

                    if (valuesStack.peek() instanceof SearchTerm) {
                        term = new AndTerm(term, (SearchTerm) valuesStack.pop());
                    }
                    resultTerm = new NotTerm(term);
                    valuesStack.push(resultTerm);

                } else {
                    searchTermBuilder = SearchTermBuilder.create(key);
                    int numOfParams = key.getNumberOfParameters();

                    for (int i = 0; i < numOfParams; i++) {
                        searchTermBuilder.addParameter(valuesStack.pop());
                    }

                    if (!searchTermBuilder.expectsParameter()) {
                        resultTerm = searchTermBuilder.build();
                        valuesStack.push(resultTerm);
                    }
                }

                if (numOfSubsequentSubjectTerms == 3) {
                    resultTerm = new AndTerm((SearchTerm) valuesStack.pop(), (SearchTerm) valuesStack.pop());
                    valuesStack.push(resultTerm);
                    numOfSubsequentSubjectTerms = 1;
                }

            } catch (Exception ex) {
                if (item.contains(spaceCharReplacement)) {
                    item = item.replaceAll(spaceCharReplacement, " ");
                }
                valuesStack.push(item);
                parsedCommandIndex++;
            }
        }

        // either create AND term or consume bad commands until ALL command
        while (valuesStack.size() != 1) {
            try {
                valuesStack.push(new AndTerm((SearchTerm) valuesStack.pop(), (SearchTerm) valuesStack.pop()));
            } catch (Exception ex) {
                log.warn("Ignoring not yet implemented command " + "'" + searchWords.get(parsedCommandIndex) + "'", ex);
            }
        }

        return (SearchTerm) valuesStack.pop();
    }

    private String removeParenthesesIfPresent(String searchString) {
        return searchString.replaceAll("\\(|\\)", "");
    }

    private ArrayList<String> stringToTermsList(String searchRequestString) {
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(searchRequestString.trim().split("\\s")));
        Collections.reverse(itemList);
        return itemList;
    }

    private String removeQuotesAndSpace(String searchRequestString) {
        Pattern quotesPattern = Pattern.compile("\".*\"");
        Matcher quotesPatternMatcher = quotesPattern.matcher(searchRequestString);

        if (quotesPatternMatcher.find()) {
            String found = quotesPatternMatcher.group(0);
            String withoutQuotes = found.replaceAll("\"", "");
            withoutQuotes = withoutQuotes.replaceAll("\\s", spaceCharReplacement);
            searchRequestString = searchRequestString.replaceAll(found, withoutQuotes);
        }

        return searchRequestString;
    }

    private String getFullSearchRequest(ImapRequestLineReader request) throws ProtocolException {
        String searchRequest = "";
        char next = request.nextChar();
        while (next != '\n') {
            searchRequest += next;
            request.consume();
            next = request.nextChar();
        }
        return searchRequest;
    }
}
