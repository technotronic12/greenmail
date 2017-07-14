/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test.commands;

import com.icegreen.greenmail.imap.commands.SearchKey;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.*;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class ImapSearchTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);
    private GreenMailUser user;
    private MailFolder folder;
    private Store store;

    private Flags fooFlags = new Flags("foo");
    private Folder imapFolder;
    private Message m0;
    private Message m1;
    private Message[] imapMessages;

    @Before
    public void before() throws Exception {
        user = greenMail.setUser("to1@localhost", "pwd");
        folder = greenMail.getManagers().getImapHostManager().getFolder(user, "INBOX");

        storeSearchTestMessages(greenMail.getImap().createSession(), folder, fooFlags);
        greenMail.waitForIncomingEmail(2);

        store = greenMail.getImap().createStore();
        store.connect("to1@localhost", "pwd");
        imapFolder = store.getFolder("INBOX");
        imapFolder.open(Folder.READ_WRITE);

        Message[] imapMessages = imapFolder.getMessages();
        assertTrue(null != imapMessages && imapMessages.length == 2);

        m0 = imapMessages[0];
        m1 = imapMessages[1];
    }

    @After
    public void after() throws Exception {
        store.close();
    }

    @Test
    public void testSimpleSubjectSearch() throws MessagingException {
        imapMessages = imapFolder.search(new SubjectTerm("test0Search"));
        assertTrue(imapMessages.length == 2);
        assertTrue(imapMessages[0] == m0);
        assertTrue(imapMessages[1] == m1);
    }

    @Test
    public void testCaseInsensitiveSubjectSearch() throws MessagingException {
        imapMessages = imapFolder.search(new SubjectTerm("TeSt0Search")); // Case insensitive
        assertTrue(imapMessages.length == 2);
        assertTrue(imapMessages[0] == m0);
    }

    @Test
    public void testPartialSubjectSearch() throws MessagingException {
        imapMessages = imapFolder.search(new SubjectTerm("1S"));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m1);

        imapMessages = imapFolder.search(new SubjectTerm("test"));
        assertTrue(imapMessages.length == 2);
    }

    @Test
    public void testExactSubjectTermSearch() throws MessagingException {
        imapMessages = imapFolder.search(new SubjectTerm("test0Search test1Search"));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m1);
    }

    @Test
    public void testNotExistingSubjectSearch() throws MessagingException {
        imapMessages = imapFolder.search(new SubjectTerm("not found"));
        assertTrue(imapMessages.length == 0);
    }

    @Test
    public void testAndTermSearch() throws MessagingException {
        imapMessages = imapFolder.search(new AndTerm(new SubjectTerm("test0Search"), new SubjectTerm("test1Search")));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m1);
    }

    @Test
    public void testOrTermSearch() throws MessagingException {
        imapMessages = imapFolder.search(new OrTerm(new SubjectTerm("test0Search"), new SubjectTerm("String2")));
        assertTrue(imapMessages.length == 2);
        assertTrue(imapMessages[0] == m0);
        assertTrue(imapMessages[1] == m1);
    }

    @Test
    public void testOrWithAndSearch() throws MessagingException {
        AndTerm andTermOfSubjects = new AndTerm(new SubjectTerm("test0Search"), new SubjectTerm("test1search"));
        imapMessages = imapFolder.search(new OrTerm(andTermOfSubjects, new SubjectTerm("something")));
        assertTrue(imapMessages.length == 1);
        assertTrue(imapMessages[0] == m1);
    }
//
//
//
//    @Test
//    public void testSearchTo() throws MessagingException {
//        imapMessages = imapFolder.search(new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to2@localhost")));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m0);
//
//        imapMessages = imapFolder.search(new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to3@localhost")));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m1);
//    }
//
//    @Test
//    public void testSearchFrom() throws MessagingException {
//        imapMessages = imapFolder.search(new FromTerm(new InternetAddress("from2@localhost")));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m0);
//
//        imapMessages = imapFolder.search(new FromTerm(new InternetAddress("from3@localhost")));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m1);
//    }
//
//    @Test
//    public void testFlags() throws Exception {
//        imapMessages = imapFolder.search(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m0);
//
//        imapMessages = imapFolder.search(new FlagTerm(fooFlags, true));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0].getFlags().contains("foo"));
//
//        imapMessages = imapFolder.search(new FlagTerm(fooFlags, false));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(!imapMessages[0].getFlags().contains(fooFlags));
//    }
//
//    @Test
//    public void testHeaderMessageId() throws Exception {
//        String id = m0.getHeader("Message-ID")[0];
//        imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m0);
//
//        id = m1.getHeader("Message-ID")[0];
//        imapMessages = imapFolder.search(new HeaderTerm("Message-ID", id));
//        assertTrue(imapMessages.length == 1);
//        assertTrue(imapMessages[0] == m1);
//    }
//
//    // Test an unsupported search term for exception. Should be ignored.
//    @Test
//    public void testUnsupportedSearchWarnsButDoesNotThrowException() throws MessagingException {
//        try {
//            SearchKey.valueOf("SENTDATE");
//            fail("Expected IAE for unimplemented search");
//        } catch (IllegalArgumentException ex) {
//            // Expected
//        }
//
//        greenMail.setUser("to1@localhost", "pwd"); // Create user
//
//        final Store store = greenMail.getImap().createStore();
//        store.connect("to1@localhost", "pwd");
//        try {
//            Folder imapFolder = store.getFolder("INBOX");
//            imapFolder.open(Folder.READ_WRITE);
//            imapFolder.search(new SentDateTerm(ComparisonTerm.LT, new Date()));
//        } finally {
//            store.close();
//        }
//    }

    /**
     * Create the two messages with different recipients, etc. for testing and add them to the folder.
     *
     * @param session Session to set on the messages
     * @param folder  Folder to add to
     * @param flags   Flags to set on both messages
     * @throws Exception if something is wrong
     */
    private static void storeSearchTestMessages(Session session, MailFolder folder, Flags flags) throws Exception {
        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject("test0Search");
        message1.setText("content");
        setRecipients(message1, Message.RecipientType.TO, "to", 1, 2);
        setRecipients(message1, Message.RecipientType.CC, "cc", 1, 2);
        setRecipients(message1, Message.RecipientType.BCC, "bcc", 1, 2);
        message1.setFrom(new InternetAddress("from2@localhost"));
        message1.setFlag(Flags.Flag.ANSWERED, true);
        message1.setFlags(flags, true);
        folder.store(message1);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("test0Search test1Search");
        message2.setText("content");
        setRecipients(message2, Message.RecipientType.TO, "to", 1, 3);
        setRecipients(message2, Message.RecipientType.CC, "cc", 1, 3);
        setRecipients(message2, Message.RecipientType.BCC, "bcc", 1, 3);
        message2.setFrom(new InternetAddress("from3@localhost"));
        message2.setFlag(Flags.Flag.ANSWERED, false);
        folder.store(message2);
    }

    /**
     * Set the recipient list of this message to a list containing two recipients of the specified type.
     *
     * @param message       Message to modify
     * @param recipientType Type of recipient to set
     * @param prefix        Prefix for recipient names. For prefix "to" and indexes {1,2} the actual names will be e.g. "to1@localhost", "to2@locahost"
     * @param indexes       List of indexes for which the addresses are generated, see doc for prefix
     */
    private static void setRecipients(MimeMessage message, Message.RecipientType recipientType, String prefix, int... indexes)
            throws MessagingException {
        Address[] arr = new InternetAddress[indexes.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new InternetAddress(prefix + indexes[i] + "@localhost");
        }
        message.setRecipients(recipientType, arr);
    }

}
