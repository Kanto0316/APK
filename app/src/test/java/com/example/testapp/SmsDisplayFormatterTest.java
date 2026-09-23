package com.example.testapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmsDisplayFormatterTest {
    @Test
    public void listNumber_singleSms_isOne() {
        assertNumbers(1, 1);
    }

    @Test
    public void listNumber_twoSms_descendsFromNewestToOldest() {
        assertNumbers(2, 2, 1);
    }

    @Test
    public void listNumber_tenSms_descendsFromTenToOne() {
        assertNumbers(10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
    }

    @Test
    public void listNumber_newSms_recalculatesAllNumbersFromCurrentTotal() {
        assertNumbers(3, 3, 2, 1);
        assertNumbers(4, 4, 3, 2, 1);
    }

    @Test
    public void sender_formatsCompatibleMalagasyNumbers() {
        assertEquals("034 14 110 58", SmsDisplayFormatter.sender("+261341411058"));
        assertEquals("038 19 808 00", SmsDisplayFormatter.sender("+261381980800"));
        assertEquals("034 14 110 58", SmsDisplayFormatter.sender("0341411058"));
    }

    @Test
    public void sender_preservesTextAndIncompatibleNumbers() {
        assertEquals("MVOLAvance", SmsDisplayFormatter.sender("MVOLAvance"));
        assertEquals("+33123456789", SmsDisplayFormatter.sender("+33123456789"));
        assertEquals("+26134141105", SmsDisplayFormatter.sender("+26134141105"));
    }

    private static void assertNumbers(int total, Integer... expected) {
        Integer[] actual = new Integer[total];
        for (int position = 0; position < total; position++) {
            actual[position] = SmsDisplayFormatter.listNumber(total, position);
        }
        assertArrayEquals(expected, actual);
    }
}
