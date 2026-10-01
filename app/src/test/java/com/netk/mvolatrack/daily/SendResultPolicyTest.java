package com.netk.mvolatrack.daily;

import org.junit.Test;
import static org.junit.Assert.*;

public class SendResultPolicyTest {
    @Test public void multipartIsAcceptedOnlyAfterEverySegmentSucceeded(){
        assertFalse(SendResultPolicy.allSegmentsAccepted(3,0));
        assertFalse(SendResultPolicy.allSegmentsAccepted(3,2));
        assertTrue(SendResultPolicy.allSegmentsAccepted(3,3));
    }

    @Test public void failureKeepsAndroidAndRadioDiagnosticCodes(){
        assertEquals("Android a refusé un segment (code 0)",
                SendResultPolicy.failureReason(0,null));
        assertEquals("Android a refusé un segment (code 1), diagnostic radio 42",
                SendResultPolicy.failureReason(1,42));
    }
}
