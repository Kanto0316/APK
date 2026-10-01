package com.netk.mvolatrack.daily;

import com.netk.mvolatrack.database.SmsMessage;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.Calendar;
import static org.junit.Assert.*;

public class DailySummaryTest {
    private static long at(int day,int hour,int minute){Calendar c=Calendar.getInstance(DailySummaryCalculator.BUSINESS_ZONE);c.clear();c.set(2026,Calendar.SEPTEMBER,day,hour,minute);return c.getTimeInMillis();}
    private static SmsMessage sms(String body,long received){return SmsMessage.create("MVola",body,received,false);}
    @Test public void respectsHalfOpenMidnightAndTypesClientsBonusAndDuplicates(){
        long day=DailySummaryCalculator.startOfDay(at(30,12,0));
        SmsMessage deposit=sms("Vous avez crédité Alice (034 00 000 01) de 1 200 Ar le 30/09/2026 à 00:00. Bonus : 12 Ar.",at(30,0,1));
        SmsMessage withdrawal=sms("2 000 Ar reçu de Alice 0340000001 le 30/09/2026 à 23:59. Bonus : 20 Ar.",at(30,23,59));
        SmsMessage credit=sms("Achat de crédit YAS réussi : 500 Ar pour 0340000002.",at(30,9,0));
        SmsMessage tomorrow=sms("Achat de crédit YAS réussi : 999 Ar pour 0340000003.",at(31,0,0));
        DailySummary s=DailySummaryCalculator.calculate(Arrays.asList(deposit,withdrawal,credit,deposit,tomorrow),day);
        assertEquals(3,s.transactions);assertEquals(2,s.clients);assertEquals(1,s.deposits);
        assertEquals(1,s.withdrawals);assertEquals(1,s.credits);assertEquals(1200,s.depositAmount);
        assertEquals(2000,s.withdrawalAmount);assertEquals(500,s.creditAmount);assertEquals(32,s.bonus);
        assertTrue(s.bonusPartial);
    }
    @Test public void emptyDayDoesNotInventValues(){DailySummary s=DailySummaryCalculator.calculate(Collections.emptyList(),at(30,0,0));assertEquals(0,s.transactions);assertEquals(0,s.bonus);assertFalse(s.bonusPartial);}
    @Test public void validatesAndFormatsTemplate(){DailySummary s=new DailySummary(at(30,0,0));s.depositAmount=1234567;s.bonusPartial=true;assertNull(DailySummaryTemplate.validationError("{date} {montant_depots}"));String rendered=DailySummaryTemplate.render("{montant_depots} {bonus}",s);assertTrue(rendered.contains("Ar"));assertTrue(rendered.contains("partiel"));assertNotNull(DailySummaryTemplate.validationError("{inconnue}"));assertNotNull(DailySummaryTemplate.validationError("{date"));}
}
