package com.netk.mvolatrack.overlay;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionPresentationQueueTest {
    private static final class Item implements TransactionPresentationQueue.Identified {
        final long id;
        Item(long id) { this.id = id; }
        @Override public long id() { return id; }
    }

    @Test public void fifoDoesNotReplaceVisibleTransaction() {
        TransactionPresentationQueue<Item> queue = new TransactionPresentationQueue<>();
        Item a = new Item(1), b = new Item(2), c = new Item(3);
        assertTrue(queue.offer(a, a.id));
        assertSame(a, queue.acquireNext());
        assertTrue(queue.offer(b, b.id));
        assertTrue(queue.offer(c, c.id));
        assertSame(a, queue.acquireNext());
        assertEquals(2, queue.pendingCount());
        assertSame(b, queue.acknowledge(a.id));
        assertSame(c, queue.acknowledge(b.id));
        assertNull(queue.acknowledge(c.id));
    }

    @Test public void duplicateDeliveryOrNotificationClickIsQueuedOnlyOnce() {
        TransactionPresentationQueue<Item> queue = new TransactionPresentationQueue<>();
        Item item = new Item(42);
        assertTrue(queue.offer(item, item.id));
        assertFalse(queue.offer(new Item(42), 42));
        assertSame(item, queue.acquireNext());
        assertTrue(queue.isShowing());
        assertEquals(0, queue.pendingCount());
    }
}
