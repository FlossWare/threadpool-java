package org.flossware.jthreadpool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ManagedThreadPool uncaught exception handler.
 */
class ManagedThreadPoolUncaughtExceptionTest {

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
    @DisplayName("Should handle uncaught exceptions via Thread.UncaughtExceptionHandler")
    void testUncaughtExceptionHandler() throws Exception {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1)
                .maxPoolSize(1)
                .build();
        pool = new ManagedThreadPool("uncaught-handler-test", config);

        // Get the executor's thread factory
        Field executorField = ManagedThreadPool.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorField.get(pool);

        // Get the thread factory
        ThreadFactory factory = executor.getThreadFactory();
        assertNotNull(factory, "Thread factory should not be null");

        // Create a thread using the factory
        Runnable dummyTask = () -> {};
        Thread thread = factory.newThread(dummyTask);
        assertNotNull(thread, "Created thread should not be null");

        // Get the uncaught exception handler
        Thread.UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
        assertNotNull(handler, "Uncaught exception handler should be set");

        // Manually invoke the handler to test it
        RuntimeException testException = new RuntimeException("Test uncaught exception");
        assertDoesNotThrow(() -> handler.uncaughtException(thread, testException),
                "Uncaught exception handler should not throw");
    }
}
