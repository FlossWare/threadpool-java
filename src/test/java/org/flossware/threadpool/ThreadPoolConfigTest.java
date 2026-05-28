package org.flossware.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThreadPoolConfig and its Builder.
 */
class ThreadPoolConfigTest {

    @Test
    @DisplayName("Should create config with default values")
    void testDefaultConfig() {
        ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();

        assertEquals(2, config.getCorePoolSize());
        assertEquals(10, config.getMaxPoolSize());
        assertEquals(60, config.getKeepAliveTimeSeconds());
        assertEquals(100, config.getQueueCapacity());
    }

    @Test
    @DisplayName("Should create config with custom values")
    void testCustomConfig() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(5)
                .maxPoolSize(20)
                .keepAliveTimeSeconds(120)
                .queueCapacity(200)
                .build();

        assertEquals(5, config.getCorePoolSize());
        assertEquals(20, config.getMaxPoolSize());
        assertEquals(120, config.getKeepAliveTimeSeconds());
        assertEquals(200, config.getQueueCapacity());
    }

    @Test
    @DisplayName("Should allow core pool size equal to max pool size")
    void testCoreEqualsMax() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(10)
                .maxPoolSize(10)
                .build();

        assertEquals(10, config.getCorePoolSize());
        assertEquals(10, config.getMaxPoolSize());
    }

    @Test
    @DisplayName("Should allow zero core pool size")
    void testZeroCorePoolSize() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(0)
                .maxPoolSize(10)
                .build();

        assertEquals(0, config.getCorePoolSize());
    }

    @Test
    @DisplayName("Should allow zero keep alive time")
    void testZeroKeepAliveTime() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .keepAliveTimeSeconds(0)
                .build();

        assertEquals(0, config.getKeepAliveTimeSeconds());
    }

    @Test
    @DisplayName("Should allow zero queue capacity")
    void testZeroQueueCapacity() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .queueCapacity(0)
                .build();

        assertEquals(0, config.getQueueCapacity());
    }

    @Test
    @DisplayName("Should reject negative core pool size")
    void testNegativeCorePoolSize() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder()
                .corePoolSize(-1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("corePoolSize must be >= 0"));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    @DisplayName("Should reject zero max pool size")
    void testZeroMaxPoolSize() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder()
                .maxPoolSize(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("maxPoolSize must be > 0"));
        assertTrue(ex.getMessage().contains("0"));
    }

    @Test
    @DisplayName("Should reject negative max pool size")
    void testNegativeMaxPoolSize() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder()
                .maxPoolSize(-1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("maxPoolSize must be > 0"));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    @DisplayName("Should reject max pool size less than core pool size")
    void testMaxLessThanCore() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder()
                .corePoolSize(10)
                .maxPoolSize(5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("maxPoolSize"));
        assertTrue(ex.getMessage().contains("corePoolSize"));
        assertTrue(ex.getMessage().contains("5"));
        assertTrue(ex.getMessage().contains("10"));
    }

    @Test
    @DisplayName("Should reject negative keep alive time")
    void testNegativeKeepAliveTime() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder()
                .keepAliveTimeSeconds(-1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("keepAliveTimeSeconds must be >= 0"));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    @DisplayName("Should reject negative queue capacity")
    void testNegativeQueueCapacity() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder()
                .queueCapacity(-1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(ex.getMessage().contains("queueCapacity must be >= 0"));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    @DisplayName("Should support builder chaining")
    void testBuilderChaining() {
        ThreadPoolConfig.Builder builder = ThreadPoolConfig.builder();

        assertSame(builder, builder.corePoolSize(5));
        assertSame(builder, builder.maxPoolSize(10));
        assertSame(builder, builder.keepAliveTimeSeconds(120));
        assertSame(builder, builder.queueCapacity(200));
    }

    @Test
    @DisplayName("Should handle large values")
    void testLargeValues() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(1000)
                .maxPoolSize(10000)
                .keepAliveTimeSeconds(Long.MAX_VALUE)
                .queueCapacity(Integer.MAX_VALUE)
                .build();

        assertEquals(1000, config.getCorePoolSize());
        assertEquals(10000, config.getMaxPoolSize());
        assertEquals(Long.MAX_VALUE, config.getKeepAliveTimeSeconds());
        assertEquals(Integer.MAX_VALUE, config.getQueueCapacity());
    }

    @Test
    @DisplayName("Should allow partial configuration")
    void testPartialConfiguration() {
        ThreadPoolConfig config = ThreadPoolConfig.builder()
                .corePoolSize(5)
                .build();

        assertEquals(5, config.getCorePoolSize());
        assertEquals(10, config.getMaxPoolSize()); // default
        assertEquals(60, config.getKeepAliveTimeSeconds()); // default
        assertEquals(100, config.getQueueCapacity()); // default
    }
}
