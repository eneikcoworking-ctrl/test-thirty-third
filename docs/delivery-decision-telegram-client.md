# Technical Delivery Decision Record: Telegram Client Core Architecture

- **Project ID**: 54fc1d2e-1e43-4ab4-a8ac-6a111dec41ab
- **Role**: BARCAN-TAG-09 - Delivery Management / Technical Lead
- **Status**: APPROVED
- **Date**: July 22, 2026

---

## 1. Context & Objectives

To automate cold outreach and dialog qualification in Telegram, the **LeadGen Bot** platform requires a reliable, resilient, and ban-preventive integration with the Telegram API. Key core capabilities include:
1. **Guaranteed Per-Session Proxy Isolation**: Enforcing that each distinct account session traffic goes through its own designated SOCKS5/HTTP proxy. Failure or leaks must not route through the host machine's public IP address, preventing cascade/chain bans.
2. **Anti-Ban & Anti-Fraud Simulation**: Ability to perform randomized delays (e.g., 120-300 seconds), typing status simulations ("typing..."), and session metadata randomizations to closely mimic human interactions.
3. **Build & Maintenance Stability**: The architecture must minimize Java compilation overhead, native compilation issues, memory management bugs, and keep our main Java Spring Boot application secure from Native JVM crashes.

This document evaluates the trade-offs between implementing **TDLib via JNI/JNA** directly in Java versus deploying an **External Node.js/Python gRPC or REST Bridge** (using high-level libraries like GramJS, Telethon, or Pyrogram), and records the delivery decision.

---

## 2. Comprehensive Evaluation

### Option A: TDLib via JNI/JNA (Direct Java Integration)

This approach utilizes direct native bindings to compiled TDLib C++ libraries within the JVM process.

#### Strengths
- **Single-process Simplicity**: Eliminates inter-process communication overhead and keeps the service execution entirely in the Java runtime.
- **Official Telegram Baseline**: TDLib is developed and maintained by Telegram directly, ensuring comprehensive API coverage.

#### Weaknesses
- **JVM Crash Vulnerability**: Native segmentation faults, native memory leaks, or thread deadlocks within TDLib binaries directly crash the parent JVM, abruptly shutting down the entire Spring Boot service.
- **Complex Build Pipeline**: Compiling TDLib for multiple target architectures (Linux, macOS, Windows) and configuring OS-level native library linker paths (`java.library.path`) adds immense overhead to development, CI pipelines, and production environments.
- **High-level Abstraction Gap**: Configuring proxy routing, randomized typing simulations, and error states directly via TDLib raw JSON or JNI calls requires significant boilerplate, leading to custom code complexity.

---

### Option B: Node.js/Python Bridge (REST / gRPC / IPC)

This approach isolates the Telegram API client logic in a lightweight external service (e.g., written in Node.js using `GramJS` or in Python using `Pyrogram`/`Telethon`), exposing endpoints via gRPC or REST to the main Spring Boot backend.

#### Strengths
- **Resilience and Isolation**: A crash, memory leak, or rate-limiting block in a Telegram session process is isolated. The main Spring Boot backend remains fully operational, guaranteeing core system uptime.
- **Native Per-Session Proxy Isolation**: High-level libraries (e.g., Pyrogram and GramJS) provide first-class, built-in support for injecting custom proxy configurations (host, port, username, password) directly upon initializing each connection client.
- **Anti-Ban Abstractions**: Out-of-the-box API functions to trigger typing state, change profiles, and natively handle complex flood wait behaviors (such as `FLOOD_WAIT` or `PEER_FLOOD`).
- **Lean Tooling**: Substantially lower build pipeline and developer setup complexity.

#### Weaknesses
- **Operational Overhead**: Requires orchestrating and monitoring an additional runtime container/service alongside Spring Boot.
- **Network Overhead**: Minor latency penalty due to gRPC/REST serialization between the Java backend and the Bridge.

---

## 3. Systems & Lean-TOC Tradeoff Analysis

Applying our core architectural and product principles:

- **Lean Management (Muda Elimination)**: Designing custom Java JNI wrappers and setting up complex native compilation toolchains constitutes massive waste (**Muda**). Choosing Option B leverages mature, high-level wrapper libraries with native proxy and anti-ban capabilities, significantly reducing development time and maintenance overhead.
- **Theory of Constraints (TOC)**: The primary bottleneck (Constraint) is the speed and stability of setting up secure, non-leaking, proxy-bound Telegram connections. Forcing developers to manage low-level C++ bindings inside the JVM diverts focus and creates massive Work-in-Progress (WIP) overhead.
- **Six Sigma Quality**: Per-session proxy isolation must have zero leakage. In Option B, if a proxy connection fails, the external bridge client is configured to **fail-closed** rather than falling back to the host machine's public IP address, enforcing a strict anti-ban barrier.

---

## 4. Final Decision

Based on the evaluation, the system will implement **Option B: Node.js/Python Bridge (via REST / gRPC)** as the core Telegram client architecture.

This decision directly ensures:
1. **Per-Session Proxy Isolation**: Proxies are injected directly at the external client initialization level, binding all socket traffic strictly to the designated proxy with automatic fail-closed logic.
2. **Anti-Ban Security**: Leveraging Pyrogram/Telethon/GramJS built-in humanization simulation features with zero JVM crash risks.

---

## 5. Concise Handoff Note & Next Steps

This delivery decision completes the **Telegram Client Core Architecture Spike** (BARCAN-TAG-09). No implementation scope expansion is introduced.

- **Next Owner Role**: `BARCAN-TAG-08 - Database/Data Engineer`
- **Immediate Task**: Implement the Database Schema and migration scripts (`V2__campaign_and_contacts.sql` or equivalent) to store Telegram accounts, campaign configurations, contact lists, and status history logs, establishing correct database mappings.
