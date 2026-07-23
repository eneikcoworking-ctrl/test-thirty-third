# ADR-001: Telegram Client Core Architecture

* **Status:** Accepted
* **Role:** BARCAN-TAG-09 (Delivery Management / Technical Lead)
* **Deciders:** BARCAN-TAG-09
* **Next Owner Role:** BARCAN-TAG-08 (Database/Data Engineer)

---

## 1. Context and Problem Statement

The LeadGen Bot platform manages outbound campaigns over a network of warmed-up Telegram accounts. A critical requirement is to prevent chain-bans and individual spam-blocks by enforcing absolute isolation of Telegram client traffic.

When designing the core architecture, we must evaluate two main technical approaches to interface with the Telegram API from our Spring Boot (Java 21) backend:
1. **Option A:** TDLib via JNI (Java Native Interface) / JNA (Java Native Access).
2. **Option B:** External Node.js/Python gRPC or REST Bridge (utilizing high-level libraries like `Pyrogram`, `Telethon`, or `GramJS`).

---

## 2. Evaluation Criteria

* **Anti-Ban and Anti-Fraud Stability:** Seamless support for typing simulation, random request pacing, and custom MTProto/session metadata parameters to mimic human actions.
* **Per-Session Proxy Isolation:** Robust, non-leaking injection of individual IPv4/IPv6 (SOCKS5/HTTP) proxies for every unique active session.
* **Build and Maintenance Complexity:** Compilation overhead, native dependencies, and environment setup across dev/CI/production environments.
* **JVM Stability:** Mitigation of native segfaults, memory leaks, and thread-blocking issues from third-party native code.

---

## 3. Option Comparison

### Option A: TDLib via JNI/JNA

* **Overview:** Write a native integration using the official TDLib (Telegram Database Library) compiled binaries, invoking them in Java through JNI.
* **Pros:**
  * Single-language codebase (entirely Java).
  * High-performance, direct execution inside the same process space.
* **Cons:**
  * **JVM Crash Risks:** Out-of-memory errors or segmentation faults in TDLib can crash the entire JVM, terminating the Spring Boot backend.
  * **Build Pipeline Complexity:** Requires building TDLib from source for each target architecture (Linux, macOS, Windows) and setting up complex native linker paths.
  * **Proxy Management Overhead:** Configuring proxies per session via TDLib JNI requires low-level C++ structure mapping and lacks direct high-level abstractions in typical Java wrappers.

### Option B: Node.js/Python Bridge (Chosen Approach)

* **Overview:** Build a separate, lightweight microservice or worker pool in Python (using `Pyrogram` or `Telethon`) or Node.js (using `GramJS`), exposed to the Spring Boot backend via gRPC or a REST API.
* **Pros:**
  * **Isolate/Decouple Stability:** A crash or freeze in a Telegram session process does not impact the Spring Boot core backend.
  * **Extremely Stable Libraries:** Python/Node wrappers have vast community adoption and handle connection retries, state machine management, and edge-cases (like `FLOOD_WAIT`) out-of-the-box.
  * **Guaranteed Per-Session Proxy Isolation:** Libraries like Pyrogram and Telethon allow proxy injection natively per connection client instance. Network calls are bound directly to the specified proxy parameters.
  * **Anti-Fraud Abstractions:** Built-in APIs to trigger typing status ("typing..."), upload profile avatars, and easily integrate delays.
* **Cons:**
  * Serialization and networking overhead (gRPC/REST/IPC).
  * An additional runtime component to package and monitor.

---

## 4. Per-Session Proxy Isolation Guarantee

Under the chosen **Option B**, every account session is treated as an isolated connection instance.

* **Mechanism:** When starting or restoring a session, the backend provides the account's credentials along with its designated SOCKS5/HTTP proxy configuration (host, port, username, password).
* **Isolation Enforcement:** The Bridge instantiates the native Telegram client (e.g., Pyrogram's `Client` or GramJS's `TelegramClient`) by passing the `proxy` object directly into the initializer. This forces the underlying socket library (e.g., `PySocks` or Node `socks-proxy-agent`) to route all MTProto traffic solely through that specific proxy.
* **Zero Leakage:** In the event of a proxy failure, the client instance is configured to fail-closed (raise a connection error) rather than falling back to the host machine's public IP address, preventing chain ban contamination.

---

## 5. Required Runtime Dependencies & Deployment Evaluation

To support Option B (External Python/Node.js Bridge) in production and development, the following runtime dependencies and deployment configurations are defined:

1. **Runtime Interpreters**:
   - **Python 3.11+** or **Node.js 20 LTS** as the runtime execution environment for the external bridge service.
2. **Core Telegram & Proxy Libraries**:
   - **Pyrogram 2.0+** or **GramJS 2.0+** to implement the MTProto protocol interface.
   - **PySocks** (for Python) or **socks-proxy-agent** (for Node.js) to manage socket-level SOCKS5/HTTP proxies without system-wide routing changes.
3. **Integration Protocol**:
   - **gRPC** (via standard protobuf definitions) or **REST API** (using JSON payloads over HTTP/2 or HTTP/1.1) to bridge the Spring Boot backend and the external client service.
4. **Proxy Networking Rule (Zero Leakage)**:
   - Dynamic proxy binding at the socket layer per-session initialization. The MTProto client is configured with a **fail-closed** policy, ensuring that if a proxy fails, all traffic halts and throws a connection exception immediately, with absolutely no fallback to the host machine's public IP address.

---

## 6. Architectural Decision

We will implement **Option B: Node.js/Python Bridge (specifically Python with Pyrogram/Telethon or Node with GramJS via gRPC/REST)**.

**Rationale:**
* **Safety First:** Prevents native memory issues from crashing the central Spring Boot engine.
* **Speed to Market:** Greatly simplifies build and CI/CD pipelines by avoiding native TDLib JNI compiles.
* **First-Class Proxy Support:** Delivers bulletproof, per-session proxy bindings natively supported by mature libraries.

---

## 7. Handoff Note

* **Current Status:** Completed Core Architecture Spike (BARCAN-TAG-09).
* **Next Owner Role:** `BARCAN-TAG-08` (Database/Data Engineer).
* **Next Step Scope:**
  1. Define the persistence layer (Database Schema) for tracking account session status, proxy associations, and credential paths to store Telegram accounts, proxies, and session status.
  2. Implement the migration scripts (`V2__accounts_and_proxies.sql` or equivalent) to establish proper database mappings and constraints ensuring one proxy is uniquely assigned to one account.
  3. No additional implementation scope or adjacent slices are scheduled for this phase to preserve strict Lean focus.
