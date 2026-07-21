# Technical Delivery Decision Record: LLM Integration Strategy

- **Project ID**: 54fc1d2e-1e43-4ab4-a8ac-6a111dec41ab
- **Role**: BARCAN-TAG-09 - Delivery Management / Technical Lead
- **Status**: APPROVED
- **Date**: July 21, 2026

---

## 1. Context & Objectives

The LeadGen Bot system aims to automate cold outreach and lead qualification in Telegram. A key capability is the **LLM Dialog Engine**, which automatically responds to user replies to qualify leads. To achieve this without manual operator effort, the system requires:
1. **Dynamic System Prompts**: Ability to dynamically load and format system instructions (e.g., business role, tone of voice, objective, client-specific info) at runtime per campaign or dialog session.
2. **Robust Intent Detection**: Highly reliable classification of incoming messages to detect key events (such as readiness to have a call, sharing contact info, negative reaction, or request to unsubscribe), which triggers predefined stop-actions or human interception.

This document evaluates the trade-offs between utilizing **Spring AI** versus writing a **Direct HTTP Client** (such as Spring's standard `RestClient` or `WebClient`) targeting OpenAI/Anthropic APIs, and records the delivery decision.

---

## 2. Comprehensive Evaluation

### Option A: Spring AI

Spring AI is an application framework designed to simplify the development of AI-driven applications by providing unified abstractions across multiple model providers.

#### Strengths
- **Dynamic System Prompts**: Provides native, structured prompt templates (e.g., `SystemPromptTemplate` and `PromptTemplate`) with map-based substitution. This simplifies campaign-specific system instructions on the fly.
- **Robust Intent Detection / Structured Outputs**: Built-in support for structured output parser abstractions like `BeanOutputConverter` and `MapOutputConverter`. Under the hood, Spring AI constructs the exact JSON schema definition requested by OpenAI/Anthropic structured outputs (or function calling) and handles response validation and parsing into custom Java Records/POJOs automatically.
- **Unified Interface**: A single Java API can target both OpenAI and Anthropic, allowing easy provider switching at runtime without rewriting code.

#### Weaknesses
- **Lifecycle & Stability**: Spring AI is currently in milestone/release-candidate phases, meaning APIs are newer and can experience minor changes compared to standard Spring Boot core modules.
- **Dependency Footprint**: Introduces additional starter dependencies.

---

### Option B: Direct HTTP Client (via Spring RestClient)

This approach involves building custom REST client wrappers using Spring 6's modern `RestClient` or Spring WebFlux's `WebClient` to invoke the OpenAI or Anthropic chat completion endpoints directly.

#### Strengths
- **Production-Ready & Minimalist**: Utilizes standard, highly stable Spring Core modules with zero additional external dependencies, avoiding version alignment issues or experimental starters.
- **Payload Level Control**: Complete and direct control over the exact HTTP request/response payloads, headers, and connection parameters.

#### Weaknesses
- **Heavy Boilerplate (Muda)**: Requires hand-crafting JSON payloads, manually managing system prompt templates via basic string interpolation, and writing custom logic to specify JSON schemas for structured output.
- **Brittle Intent Detection**: Developers must write custom validation and error-handling code to handle cases where the LLM's response fails to conform to the requested JSON schema structure, increasing the likelihood of runtime bugs and unhandled parsing exceptions.

---

## 3. Systems & Lean-TOC Tradeoff Analysis

Applying our core architectural and product principles:

- **Lean Management (Muda Elimination)**: Hand-crafting JSON schemas, HTTP requests, and custom JSON schema validation layers represents substantial **waste (Muda)**. Reusing Spring AI's well-tested structured output converter abstractions allows developers to bypass custom parser implementation, accelerating time-to-market.
- **Theory of Constraints (TOC)**: The primary bottleneck in building the Dialog Engine is the implementation speed of resilient dialog flow logic and reliable intent parsing. Implementing a direct HTTP client would distract the engineering team from the core qualification logic and accumulate Work-In-Progress (WIP).
- **Six Sigma Quality**: Intent detection accuracy must be extremely high. Spring AI's native integration with LLM structured outputs (via JSON Schema and JSON Mode) guarantees that LLM responses strictly conform to our `IntentDetectionResult` Java Records.

---

## 4. Final Decision

Based on the evaluation, the system will use **Spring AI** as the primary integration framework for LLMs.

This decision strictly supports:
1. **Dynamic System Prompts** through Spring AI's flexible template rendering engine (`SystemPromptTemplate`).
2. **Robust Intent Detection** by defining a precise Java Record for intent metadata and utilizing `BeanOutputConverter` to enforce structural compliance on LLM replies.

---

## 5. Concise Handoff Note & Next Steps

This delivery decision has been completed by the Systems Analyst / Technical Lead. No implementation scope expansion is introduced; this spike focuses strictly on the AI Integration Strategy.

- **Next Owner Role**: `BARCAN-TAG-01 - Backend Developer`
- **Immediate Task**: Configure the Maven dependencies for Spring AI in the backend module, implement a prototype of `LLMDialogEngine` utilizing `SystemPromptTemplate` and `BeanOutputConverter`, and verify structured intent output.
