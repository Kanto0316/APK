package com.netk.mvolatrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small, UI-independent helper that limits transaction rows created for a page. */
final class TransactionPaginator {
    static final int PAGE_SIZE = 50;

    private TransactionPaginator() {}

    static int pageCount(int itemCount) {
        return itemCount <= 0 ? 0 : (itemCount + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    static int clampPage(int requestedPage, int itemCount) {
        int pages = pageCount(itemCount);
        if (pages == 0) return 0;
        return Math.max(0, Math.min(requestedPage, pages - 1));
    }

    static <T> List<T> page(List<T> items, int page) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        int safePage = clampPage(page, items.size());
        int from = safePage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        return new ArrayList<>(items.subList(from, to));
    }
}
