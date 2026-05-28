package org.flossware.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThreadPoolStats.
 */
class ThreadPoolStatsTest {

    @Test
    @DisplayName("Should create stats with all fields")
    void testConstructorAndGetters() {
        ThreadPoolStats stats = new ThreadPoolStats(5, 100L, 10, 8, 4, 16);

        assertEquals(5, stats.getActiveThreads());
        assertEquals(100L, stats.getCompletedTasks());
        assertEquals(10, stats.getQueuedTasks());
        assertEquals(8, stats.getPoolSize());
        assertEquals(4, stats.getCorePoolSize());
        assertEquals(16, stats.getMaximumPoolSize());
    }

    @Test
    @DisplayName("Should handle zero values")
    void testZeroValues() {
        ThreadPoolStats stats = new ThreadPoolStats(0, 0L, 0, 0, 0, 0);

        assertEquals(0, stats.getActiveThreads());
        assertEquals(0L, stats.getCompletedTasks());
        assertEquals(0, stats.getQueuedTasks());
        assertEquals(0, stats.getPoolSize());
        assertEquals(0, stats.getCorePoolSize());
        assertEquals(0, stats.getMaximumPoolSize());
    }

    @Test
    @DisplayName("Should handle large completed task count")
    void testLargeCompletedTasks() {
        long largeCount = Long.MAX_VALUE;
        ThreadPoolStats stats = new ThreadPoolStats(1, largeCount, 0, 1, 1, 1);

        assertEquals(largeCount, stats.getCompletedTasks());
    }

    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        ThreadPoolStats stats = new ThreadPoolStats(5, 100L, 10, 8, 4, 16);

        String result = stats.toString();

        assertTrue(result.contains("ThreadPoolStats"));
        assertTrue(result.contains("active=5"));
        assertTrue(result.contains("completed=100"));
        assertTrue(result.contains("queued=10"));
        assertTrue(result.contains("poolSize=8"));
        assertTrue(result.contains("core=4"));
        assertTrue(result.contains("max=16"));
    }

    @Test
    @DisplayName("Should format toString with zero values")
    void testToStringZeroValues() {
        ThreadPoolStats stats = new ThreadPoolStats(0, 0L, 0, 0, 0, 0);

        String result = stats.toString();

        assertTrue(result.contains("active=0"));
        assertTrue(result.contains("completed=0"));
        assertTrue(result.contains("queued=0"));
        assertTrue(result.contains("poolSize=0"));
        assertTrue(result.contains("core=0"));
        assertTrue(result.contains("max=0"));
    }

    @Test
    @DisplayName("Should handle maximum integer values")
    void testMaximumValues() {
        ThreadPoolStats stats = new ThreadPoolStats(
            Integer.MAX_VALUE,
            Long.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        );

        assertEquals(Integer.MAX_VALUE, stats.getActiveThreads());
        assertEquals(Long.MAX_VALUE, stats.getCompletedTasks());
        assertEquals(Integer.MAX_VALUE, stats.getQueuedTasks());
        assertEquals(Integer.MAX_VALUE, stats.getPoolSize());
        assertEquals(Integer.MAX_VALUE, stats.getCorePoolSize());
        assertEquals(Integer.MAX_VALUE, stats.getMaximumPoolSize());
    }
}
