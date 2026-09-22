package com.example.testapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DepositRecipientSearchTest {
    private final DepositRecipient jean = new DepositRecipient(
            "0341411058", "Jean Rakoto", 0L, true);

    @Test public void findsAContactByNameNationalAndInternationalNumber() {
        List<DepositRecipient> contacts = Collections.singletonList(jean);
        assertEquals("0341411058", DepositRecipientSearch.find(contacts,
                Collections.emptyList(), "Jean", 8).get(0).number);
        assertEquals("0341411058", DepositRecipientSearch.find(contacts,
                Collections.emptyList(), "03414", 8).get(0).number);
        assertEquals("0341411058", DepositRecipientSearch.find(contacts,
                Collections.emptyList(), "+26134", 8).get(0).number);
    }

    @Test public void deDuplicatesContactAndRecentWhileKeepingContactName() {
        DepositRecipient recent = new DepositRecipient("0341411058", "", 42L, false);
        List<DepositRecipient> result = DepositRecipientSearch.find(
                Collections.singletonList(jean), Collections.singletonList(recent), "034", 8);
        assertEquals(1, result.size());
        assertEquals("Jean Rakoto", result.get(0).name);
        assertEquals(42L, result.get(0).lastUsed);
    }

    @Test public void emptyQueryShowsOnlyMostRecentFive() {
        List<DepositRecipient> recent = Arrays.asList(
                recipient("0320000001", 1), recipient("0320000002", 2),
                recipient("0320000003", 3), recipient("0320000004", 4),
                recipient("0320000005", 5), recipient("0320000006", 6));
        List<DepositRecipient> result = DepositRecipientSearch.find(
                Collections.singletonList(jean), recent, "", 5);
        assertEquals(5, result.size());
        assertEquals("0320000006", result.get(0).number);
    }

    private static DepositRecipient recipient(String number, long lastUsed) {
        return new DepositRecipient(number, "", lastUsed, false);
    }
}
