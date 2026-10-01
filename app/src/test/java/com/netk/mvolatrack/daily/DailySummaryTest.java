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
    @Test public void balanceIsRenderedWhenPresent(){
        DailySummary s=DailySummaryCalculator.calculate(Collections.singletonList(sms(
                "Vous avez crédité Alice (0340000001) de 1 200 Ar le 30/09/2026 à 08:15. Solde MVola : 45 600 Ar.",at(30,8,16))),at(30,0,0));
        assertEquals(Long.valueOf(45600),s.balance);
        assertEquals("45\u202f600 Ar",DailySummaryTemplate.render("{solde}",s));
    }
    @Test public void latestTransactionBalanceWinsRegardlessOfSmsOrder(){
        SmsMessage latest=sms("2 000 Ar reçu de Alice 0340000001 le 30/09/2026 à 18:45. Solde : 90 000 Ar.",at(30,18,46));
        SmsMessage earlier=sms("Vous avez crédité Bob (0340000002) de 500 Ar le 30/09/2026 à 07:10. Solde : 12 000 Ar.",at(30,20,0));
        DailySummary s=DailySummaryCalculator.calculate(Arrays.asList(latest,earlier),at(30,0,0));
        assertEquals(Long.valueOf(90000),s.balance);
    }
    @Test public void nextDayBalanceIsExcluded(){
        SmsMessage today=sms("Vous avez crédité Alice (0340000001) de 100 Ar le 30/09/2026 à 23:58. Solde : 8 000 Ar.",at(30,23,59));
        SmsMessage tomorrow=sms("Vous avez crédité Bob (0340000002) de 100 Ar le 01/10/2026 à 00:01. Solde : 99 000 Ar.",at(30,23,59));
        DailySummary s=DailySummaryCalculator.calculate(Arrays.asList(tomorrow,today),at(30,0,0));
        assertEquals(Long.valueOf(8000),s.balance);
    }
    @Test public void missingBalanceIsUnavailableRatherThanZero(){
        DailySummary s=DailySummaryCalculator.calculate(Collections.singletonList(sms(
                "Achat de crédit YAS réussi : 500 Ar pour 0340000002.",at(30,9,0))),at(30,0,0));
        assertNull(s.balance);
        assertEquals("indisponible",DailySummaryTemplate.render("{solde}",s));
    }
    @Test public void validatesAndFormatsTemplate(){DailySummary s=new DailySummary(at(30,0,0));s.depositAmount=1234567;s.bonusPartial=true;assertNull(DailySummaryTemplate.validationError("{date} {montant_depots} {solde}"));String rendered=DailySummaryTemplate.render("{montant_depots} {bonus}",s);assertTrue(rendered.contains("Ar"));assertTrue(rendered.contains("partiel"));assertNotNull(DailySummaryTemplate.validationError("{inconnue}"));assertNotNull(DailySummaryTemplate.validationError("{date"));}
}
