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

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.search.*;
import java.nio.charset.CharacterCodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles processing for the SEARCH imap command.
 * @author Eli Pony <elipony@outlook.com>
 */
class SearchCommandParser extends CommandParser {
    private final Logger log = LoggerFactory.getLogger(SearchCommandParser.class);
    private final String spaceCharReplacement = "_SPACE_CHAR_REPLACEMENT_";
    private static final String CHARSET_TOKEN = "CHARSET";

    /**
     * Parses the request argument into a valid search term. Not yet fully implemented - see SearchKey enum.
     * <p>
     * Other searches will return everything for now.
     */
    public SearchTerm searchTerm(ImapRequestLineReader request) throws ProtocolException, CharacterCodingException {
        SearchTerm resultTerm;
        Stack<String> valuesStack = new Stack<>();

        String searchRequestString = getFullSearchRequest(request);
        searchRequestString = removeQuotesAndSpace(searchRequestString);
        searchRequestString = removeParenthesesIfPresent(searchRequestString);
        ArrayList<String> searchWords = stringToTermsList(searchRequestString);
        removeAllSearchTermIfOtherSearchPresent(searchWords);
        ArrayList<SearchTerm> multipleSearchTerms = new ArrayList<>();

        for (String item : searchWords) {
            try {
                SearchKey key = SearchKey.valueOf(item);

                if (key == SearchKey.SUBJECT) {
                    resultTerm = new SubjectTerm(valuesStack.pop());
                    multipleSearchTerms.add(resultTerm);
                } else if (key == SearchKey.OR) {
                    resultTerm = new OrTerm(multipleSearchTerms.get(0), multipleSearchTerms.get(1));
                    replaceDouWithSingleTerm(resultTerm, multipleSearchTerms);
                } else if (key == SearchKey.TO) {
                    resultTerm = new RecipientTerm(Message.RecipientType.TO, new InternetAddress(valuesStack.pop()));
                    multipleSearchTerms.add(resultTerm);
                }
            } catch (Exception ex) {
                if (item.contains(spaceCharReplacement)) {
                    item = item.replaceAll(spaceCharReplacement, " ");
                }
                valuesStack.push(item);
            }
        }

        if (multipleSearchTerms.size() == 2) {
            resultTerm = new AndTerm(multipleSearchTerms.get(0), multipleSearchTerms.get(1));
            replaceDouWithSingleTerm(resultTerm, multipleSearchTerms);
        }

        return multipleSearchTerms.get(0);
    }

    private String removeParenthesesIfPresent(String searchString) {
        return searchString.replaceAll("\\(|\\)", "");
    }

    private void replaceDouWithSingleTerm(SearchTerm resultTerm, ArrayList<SearchTerm> multipleSearchTerms) {
        multipleSearchTerms.remove(1);
        multipleSearchTerms.remove(0);
        multipleSearchTerms.add(resultTerm);
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

    private void removeAllSearchTermIfOtherSearchPresent(List<String> operators) {
        String possibleAll = operators.get(0);
        if (possibleAll.equals("ALL")) {
            operators.remove(0);
        }
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
