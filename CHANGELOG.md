# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0] - 2026-05-24

### Added
- Initial release of JThreadPool
- `ThreadPoolConfig` - Builder-based configuration for thread pools
  - Configurable core/max pool sizes
  - Configurable keep-alive time
  - Configurable queue capacity
  - Validation for all parameters
  - Default configuration factory method
- `ManagedThreadPool` - Application-scoped thread pool implementation
  - Named threads with application ID prefix
  - Uncaught exception handler with logging
  - CallerRunsPolicy for rejected tasks
  - Graceful shutdown with 30-second timeout
  - Force shutdown fallback
  - AutoCloseable support for try-with-resources
  - Real-time statistics tracking
- `ThreadPoolStats` - Immutable statistics snapshot
  - Active thread count
  - Completed task count
  - Queued task count
  - Current/core/maximum pool sizes
  - Human-readable toString()
- Comprehensive test coverage (10 passing tests)
- Thread-safe concurrent operations

### Features
- Automatic thread naming: `{app-id}-thread-{N}`
- Exception logging for uncaught exceptions
- Statistics snapshot API
- Graceful shutdown with timeout
- Submit Runnable and Callable tasks
- Execute tasks without Future
- Query pool state (shutdown, terminated)

[1.0]: https://github.com/FlossWare/jthreadpool/releases/tag/v1.0
