package org.flossware.threadpool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for ManagedThreadPool to achieve 100% coverage.
 */
class ManagedThreadPoolEdgeCasesTest {

    private ManagedThreadPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("Should handle uncaught exceptions in tasks")
    void testUncaughtException() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .build();
        pool = new ManagedThreadPool("exception-test", config);

        CountDownLatch exceptionLatch = new CountDownLatch(1);
        CountDownLatch nextTaskLatch = new CountDownLatch(1);

        // Submit task that throws uncaught exception
        pool.submit(() -> {
            exceptionLatch.countDown();
            throw new RuntimeException("Uncaught test exception");
        });

        // Wait for exception to occur
        assertTrue(exceptionLatch.await(5, TimeUnit.SECONDS));

        // Thread.sleep to ensure exception handler runs
        Thread.sleep(200);

        // Pool should still be functional
        pool.submit(() -> nextTaskLatch.countDown());
        assertTrue(nextTaskLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle shutdown timeout and force shutdown")
    void testShutdownTimeout() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .build();
        pool = new ManagedThreadPool("timeout-test", config);

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch blockForever = new CountDownLatch(1);

        // Submit a task that never completes
        pool.submit(() -> {
            try {
                taskStarted.countDown();
                blockForever.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for task to start
        assertTrue(taskStarted.await(5, TimeUnit.SECONDS));

        // Replace the executor with one that will timeout
        java.lang.reflect.Field executorField = ManagedThreadPool.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        ThreadPoolExecutor realExecutor = (ThreadPoolExecutor) executorField.get(pool);

        // Create a wrapper that simulates timeout
        ThreadPoolExecutor mockExecutor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {
            @Override
            public void shutdown() {
                realExecutor.shutdown();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                // Simulate timeout
                return false;
            }

            @Override
            public java.util.List<Runnable> shutdownNow() {
                return realExecutor.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return realExecutor.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return realExecutor.isTerminated();
            }
        };

        executorField.set(pool, mockExecutor);

        // Shutdown should handle timeout and call shutdownNow
        assertDoesNotThrow(() -> pool.shutdown());

        // Cleanup
        blockForever.countDown();
        realExecutor.shutdownNow();
    }

    @Test
    @DisplayName("Should handle InterruptedException during shutdown")
    void testShutdownInterrupted() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .build();
        pool = new ManagedThreadPool("interrupt-test", config);

        // Replace the executor with one that throws InterruptedException
        java.lang.reflect.Field executorField = ManagedThreadPool.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        ThreadPoolExecutor realExecutor = (ThreadPoolExecutor) executorField.get(pool);

        ThreadPoolExecutor mockExecutor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {
            @Override
            public void shutdown() {
                realExecutor.shutdown();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("Simulated interruption");
            }

            @Override
            public java.util.List<Runnable> shutdownNow() {
                return realExecutor.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return realExecutor.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return realExecutor.isTerminated();
            }
        };

        executorField.set(pool, mockExecutor);

        // Shutdown should handle interruption
        assertDoesNotThrow(() -> pool.shutdown());

        // Thread should have interrupt flag set
        assertTrue(Thread.interrupted()); // Also clears the flag

        // Cleanup
        realExecutor.shutdownNow();
    }

    @Test
    @DisplayName("Should create non-daemon threads")
    void testNonDaemonThreads() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .build();
        pool = new ManagedThreadPool("daemon-test", config);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isDaemon = new AtomicBoolean(true);

        pool.submit(() -> {
            isDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertFalse(isDaemon.get(), "Threads should not be daemon");
    }

    @Test
    @DisplayName("Should increment thread counter for each thread")
    void testThreadCounter() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(3)
                .maxPoolSize(3)
                .build();
        pool = new ManagedThreadPool("counter-test", config);

        CountDownLatch startLatch = new CountDownLatch(3);
        CountDownLatch endLatch = new CountDownLatch(1);

        // Submit tasks that will create threads
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                startLatch.countDown();
                try {
                    endLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for all threads to start
        assertTrue(startLatch.await(5, TimeUnit.SECONDS));

        // Access the thread counter via reflection
        java.lang.reflect.Field counterField = ManagedThreadPool.class.getDeclaredField("threadCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger counter =
                (java.util.concurrent.atomic.AtomicInteger) counterField.get(pool);

        // Counter should be at least 3
        assertTrue(counter.get() >= 3, "Thread counter should be >= 3, was " + counter.get());

        // Cleanup
        endLatch.countDown();
    }

    @Test
    @DisplayName("Should get queue size in stats")
    void testStatsWithQueuedTasks() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .queueCapacity(10)
                .build();
        pool = new ManagedThreadPool("queue-test", config);

        CountDownLatch blockLatch = new CountDownLatch(1);

        // Submit a blocking task to fill the single thread
        pool.submit(() -> {
            try {
                blockLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Give it time to start
        Thread.sleep(100);

        // Submit additional tasks that will queue
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {});
        }

        ThreadPoolStats stats = pool.getStats();
        assertTrue(stats.getQueuedTasks() > 0, "Should have queued tasks");

        // Cleanup
        blockLatch.countDown();
    }

    @Test
    @DisplayName("Should report completed tasks in stats")
    void testStatsWithCompletedTasks() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(2)
                .build();
        pool = new ManagedThreadPool("completed-test", config);

        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> latch.countDown());
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Give executor time to update completed count
        Thread.sleep(100);

        ThreadPoolStats stats = pool.getStats();
        assertTrue(stats.getCompletedTasks() > 0, "Should have completed tasks");
    }

    @Test
    @DisplayName("Should handle close method via AutoCloseable")
    void testCloseMethod() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .build();
        pool = new ManagedThreadPool("close-test", config);

        CountDownLatch latch = new CountDownLatch(1);
        pool.submit(() -> latch.countDown());

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        pool.close();

        assertTrue(pool.isShutdown());
    }
}
