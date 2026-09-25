package com.netk.mvolatrack.overlay;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Thread-safe FIFO that guarantees that only one transaction owns the modal at a time. */
final class TransactionPresentationQueue<T> {
    interface Identified {
        long id();
    }

    private final ArrayDeque<T> pending = new ArrayDeque<>();
    private final Set<Long> knownIds = new HashSet<>();
    private T showing;

    synchronized boolean offer(T item, long id) {
        if (!knownIds.add(id)) return false;
        pending.add(item);
        return true;
    }

    synchronized T acquireNext() {
        if (showing == null) showing = pending.poll();
        return showing;
    }

    synchronized T current() {
        return showing;
    }

    synchronized T acknowledge(long id) {
        if (showing != null && ((Identified) showing).id() == id) {
            knownIds.remove(id);
            showing = null;
        }
        return acquireNext();
    }

    synchronized boolean isShowing() {
        return showing != null;
    }

    synchronized int pendingCount() {
        return pending.size();
    }
}
