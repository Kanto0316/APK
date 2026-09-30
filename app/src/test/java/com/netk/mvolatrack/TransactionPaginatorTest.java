package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TransactionPaginatorTest {
    @Test public void pageCountHandlesPaginationBoundaries() {
        assertEquals(0, TransactionPaginator.pageCount(0));
        assertEquals(1, TransactionPaginator.pageCount(20));
        assertEquals(1, TransactionPaginator.pageCount(50));
        assertEquals(2, TransactionPaginator.pageCount(51));
        assertEquals(3, TransactionPaginator.pageCount(120));
    }

    @Test public void pageReturnsOnlyTheRequestedFiftyRows() {
        List<Integer> values = values(120);

        assertEquals(values.subList(50, 100), TransactionPaginator.page(values, 1));
        assertEquals(values.subList(100, 120), TransactionPaginator.page(values, 2));
    }

    @Test public void pageClampsAfterFilteredResultsShrink() {
        List<Integer> filtered = values(7);

        assertEquals(filtered, TransactionPaginator.page(filtered, 4));
        assertEquals(0, TransactionPaginator.clampPage(4, filtered.size()));
    }

    private static List<Integer> values(int count) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < count; index++) result.add(index);
        return result;
    }
}
