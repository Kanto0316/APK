package com.netk.mvolatrack.daily;

/** Pure rules for aggregating Android's per-segment SMS send results. */
final class SendResultPolicy {
    private SendResultPolicy() {}

    static boolean allSegmentsAccepted(int segmentCount,int successfulParts){
        return segmentCount>0 && successfulParts>=segmentCount;
    }

    static String failureReason(int resultCode,Integer modemError){
        String reason="Android a refusé un segment (code "+resultCode+")";
        return modemError==null?reason:reason+", diagnostic radio "+modemError;
    }
}
