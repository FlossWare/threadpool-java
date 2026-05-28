# JThreadPool

[![Maven Central](https://img.shields.io/maven-central/v/org.flossware/threadpool-java.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.flossware/threadpool-java)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Managed thread pools with monitoring and graceful shutdown for Java applications.

## Features

- **Configurable Thread Pools**: Flexible core/max pool sizes, queue capacity, and keep-alive times
- **Thread Naming**: Automatic thread naming with application ID for easy identification in thread dumps
- **Exception Handling**: Uncaught exception handler that logs errors
- **Statistics Tracking**: Real-time pool statistics (active threads, completed tasks, queue size)
- **Graceful Shutdown**: Configurable shutdown with timeout and force shutdown fallback
- **AutoCloseable**: Implements AutoCloseable for try-with-resources support
- **CallerRunsPolicy**: Rejected tasks run in calling thread to prevent task loss

## Installation

### Maven

```xml
<dependency>
    <groupId>org.flossware</groupId>
    <artifactId>threadpool-java</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'org.flossware:threadpool-java:1.0'
```

## Quick Start

### Basic Usage

```java
import org.flossware.threadpool-java.*;

// Create thread pool configuration
ThreadPoolConfig config = ThreadPoolConfig.builder()
    .corePoolSize(4)
    .maxPoolSize(8)
    .keepAliveTimeSeconds(60)
    .queueCapacity(100)
    .build();

// Create managed thread pool
ManagedThreadPool pool = new ManagedThreadPool("my-app", config);

// Submit tasks
pool.submit(() -> {
    System.out.println("Running in thread: " + Thread.currentThread().getName());
    // Do work...
});

// When done
pool.shutdown();
```

### Try-With-Resources

```java
ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();

try (ManagedThreadPool pool = new ManagedThreadPool("my-app", config)) {
    pool.submit(() -> doWork());
    pool.submit(() -> doMoreWork());
    // Pool automatically shuts down when exiting try block
}
```

### Monitoring Statistics

```java
ManagedThreadPool pool = new ManagedThreadPool("my-app", config);

// Submit some tasks
for (int i = 0; i < 10; i++) {
    pool.submit(() -> doWork());
}

// Get current statistics
ThreadPoolStats stats = pool.getStats();
System.out.println("Active threads: " + stats.getActiveThreads());
System.out.println("Completed tasks: " + stats.getCompletedTasks());
System.out.println("Queued tasks: " + stats.getQueuedTasks());
System.out.println("Pool size: " + stats.getPoolSize());
```

### Callable Tasks

```java
Future<String> future = pool.submit(() -> {
    // Do some computation
    return "Result";
});

String result = future.get(5, TimeUnit.SECONDS);
System.out.println("Got result: " + result);
```

## API Overview

### ThreadPoolConfig

Configuration builder for thread pool settings.

**Builder Methods:**
- `corePoolSize(int)` - Minimum threads to keep in pool (default: 2)
- `maxPoolSize(int)` - Maximum threads allowed in pool (default: 10)
- `keepAliveTimeSeconds(long)` - Time idle threads wait before terminating (default: 60)
- `queueCapacity(int)` - Maximum queued tasks (default: 100)
- `build()` - Build the configuration

**Static Methods:**
- `defaultConfig()` - Returns configuration with default values
- `builder()` - Create new builder

**Validation:**
- `corePoolSize` must be >= 0
- `maxPoolSize` must be > 0 and >= corePoolSize
- `keepAliveTimeSeconds` must be >= 0
- `queueCapacity` must be >= 0

### ManagedThreadPool

Main thread pool implementation with monitoring.

**Constructor:**
- `ManagedThreadPool(String applicationId, ThreadPoolConfig config)`

**Task Submission:**
- `submit(Runnable)` - Submit Runnable task, returns Future<?>
- `submit(Callable<T>)` - Submit Callable task, returns Future<T>
- `execute(Runnable)` - Execute Runnable without Future

**Lifecycle:**
- `shutdown()` - Graceful shutdown (waits up to 30 seconds)
- `shutdownNow()` - Force shutdown immediately
- `isShutdown()` - Check if shutdown initiated
- `isTerminated()` - Check if all tasks completed
- `close()` - AutoCloseable support (calls shutdown)

**Monitoring:**
- `getStats()` - Get current ThreadPoolStats snapshot
- `getApplicationId()` - Get application identifier

**Thread Naming:**
Threads are named as `{applicationId}-thread-{N}` for easy identification in thread dumps and monitoring tools.

### ThreadPoolStats

Immutable snapshot of thread pool statistics.

**Methods:**
- `getActiveThreads()` - Number of actively executing tasks
- `getCompletedTasks()` - Total completed tasks
- `getQueuedTasks()` - Number of tasks waiting in queue
- `getPoolSize()` - Current number of threads in pool
- `getCorePoolSize()` - Configured core pool size
- `getMaximumPoolSize()` - Configured maximum pool size
- `toString()` - Human-readable statistics

## Use Cases

1. **Web Servers**: Handle HTTP requests with bounded concurrency
2. **Background Processing**: Execute async tasks with resource limits
3. **Multi-Tenant Applications**: Per-tenant thread pools with isolation
4. **Batch Processing**: Parallel task execution with controlled parallelism
5. **Event Processing**: Asynchronous event handling

## Best Practices

### Sizing Thread Pools

```java
// CPU-intensive tasks: 1 thread per core
int cpuBound = Runtime.getRuntime().availableProcessors();
ThreadPoolConfig cpuConfig = ThreadPoolConfig.builder()
    .corePoolSize(cpuBound)
    .maxPoolSize(cpuBound)
    .build();

// I/O-intensive tasks: More threads than cores
int ioBound = Runtime.getRuntime().availableProcessors() * 2;
ThreadPoolConfig ioConfig = ThreadPoolConfig.builder()
    .corePoolSize(ioBound)
    .maxPoolSize(ioBound * 2)
    .queueCapacity(1000)
    .build();
```

### Graceful Shutdown

```java
ManagedThreadPool pool = new ManagedThreadPool("app", config);

// Register shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("Shutting down thread pool...");
    pool.shutdown();
}));
```

### Exception Handling

Uncaught exceptions in tasks are automatically logged:

```java
pool.submit(() -> {
    throw new RuntimeException("Task failed");
    // Automatically logged: [app-id] Uncaught exception in thread app-id-thread-1
});
```

### Monitoring in Production

```java
ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
monitor.scheduleAtFixedRate(() -> {
    ThreadPoolStats stats = pool.getStats();
    logger.info("Pool stats: {}", stats);
    
    // Alert if queue is growing
    if (stats.getQueuedTasks() > 80) {
        logger.warn("Thread pool queue filling up!");
    }
}, 0, 30, TimeUnit.SECONDS);
```

## Design Principles

- **Bounded Resources**: Configurable limits prevent resource exhaustion
- **Fail-Safe**: CallerRunsPolicy prevents task rejection
- **Observable**: Real-time statistics for monitoring
- **Lifecycle Management**: Clear shutdown semantics
- **Thread Safety**: All operations are thread-safe

## Requirements

- Java 21 or higher
- SLF4J API (for logging)

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Links

- [GitHub Repository](https://github.com/FlossWare/threadpool-java)
- [Issue Tracker](https://github.com/FlossWare/threadpool-java/issues)
- [Javadoc](https://javadoc.io/doc/org.flossware/threadpool-java)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.
