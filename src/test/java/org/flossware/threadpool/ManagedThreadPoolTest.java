package org.flossware.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class ManagedThreadPoolTest {

    private ManagedThreadPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
    }

    @Test
    void testCreatePool() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .keepAliveTimeSeconds(60)
                .queueCapacity(10)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        assertNotNull(pool);
        assertEquals("test-app", pool.getApplicationId());
        assertFalse(pool.isShutdown());
        assertFalse(pool.isTerminated());
    }

    @Test
    void testSubmitRunnable() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        CountDownLatch latch = new CountDownLatch(1);
        Future<?> future = pool.submit(() -> latch.countDown());

        future.get(5, TimeUnit.SECONDS);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSubmitCallable() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        Future<String> future = pool.submit(() -> "test-result");

        assertEquals("test-result", future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void testExecute() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        CountDownLatch latch = new CountDownLatch(1);
        pool.execute(() -> latch.countDown());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testShutdown() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        pool.shutdown();

        assertTrue(pool.isShutdown());
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testShutdownNow() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        pool.submit(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        pool.shutdownNow();
        assertTrue(pool.isShutdown());
    }

    @Test
    void testGetStats() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch endLatch = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                startLatch.countDown();
                try {
                    endLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.await(5, TimeUnit.SECONDS);

        ThreadPoolStats stats = pool.getStats();
        assertEquals(2, stats.getActiveThreads());
        assertEquals(2, stats.getCorePoolSize());
        assertEquals(4, stats.getMaximumPoolSize());

        endLatch.countDown();
        endLatch.countDown();
    }

    @Test
    void testMultipleTasks() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("test-app", config);

        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, counter.get());
    }

    @Test
    void testAutoCloseable() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();

        try (ManagedThreadPool autoPool = new ManagedThreadPool("auto-close-test", config)) {
            CountDownLatch latch = new CountDownLatch(1);
            autoPool.submit(() -> latch.countDown());
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testThreadNaming() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("naming-test", config);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        pool.submit(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(threadName.get().startsWith("naming-test-thread-"));
    }
}
