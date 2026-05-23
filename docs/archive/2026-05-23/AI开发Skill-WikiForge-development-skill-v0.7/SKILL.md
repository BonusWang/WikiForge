---
name: wikiforge-development
description: WikiForge project development rules for AI agents and human developers. Use when designing, coding, reviewing, refactoring, or documenting WikiForge architecture, backend services, frontend modules, Docker/CI changes, MCP/Agent/Obsidian integrations, or multi-agent collaboration work.
---

# WikiForge Development Skill

This skill keeps WikiForge development aligned when multiple AI agents or developers work in parallel.

## Start Every Task

1. Read `AGENTS.md`.
2. Read `WORKFLOW.md`.
3. Read the latest `docs/archive/YYYY-MM-DD/*归档索引-archive-index-vX.Y.md`.
4. Read the current task's relevant reference file in this skill.
5. Confirm the current MVP stage and task card state before changing code.
6. Keep changes inside the target service or document boundary.

## Architecture Rule

WikiForge uses the B option:

- MVP runtime target: small-service microservice architecture.
- First split: `wikiforge-core-service`, `wikiforge-worker-service`, `wikiforge-ui`.
- Later split: gateway, agent, connector, MCP, vector, personal-record services.
- Do not introduce Nacos, Kafka, Redis, XXL-JOB, or a full service-mesh in MVP 0/1 unless explicitly approved.

Read `references/architecture-style.md` before architecture or service-boundary work.

## Backend Rule

Backend services follow DDD-style layers:

```text
interfaces -> application -> domain <- infrastructure
```

The `domain` layer must not depend on Spring, MyBatis, HTTP clients, filesystem APIs, or LLM SDKs.

Read `references/backend-ddd-standard.md` before backend implementation.

## Frontend Rule

The frontend stays in `frontend/`, using Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router, and Axios.

Read `references/frontend-standard.md` before UI work.

## Service Ownership Rule

Each service owns its APIs, tables, migrations, configs, Dockerfile, tests, and logs. Cross-service access must use a documented API, event, or shared contract. Do not read another service's tables directly unless the latest architecture decision explicitly allows it.

Read `references/service-boundaries.md` before creating or moving service code.

## CI And Docker Rule

Every service must be independently testable, packageable, and container-buildable. Docker images must not contain user-local Windows paths, API keys, tokens, Obsidian Vault data, or Raw Sources files.

Read `references/ci-docker-standard.md` before build, Docker, or CI work.

## Collaboration Rule

WikiForge uses a lightweight Symphony-inspired workflow:

- GitHub Issue or issue-style task card is the task control plane.
- `WORKFLOW.md` defines task states, branch isolation, handoff and completion rules.
- Work Orders in `docs/superpowers/plans/` remain the executable implementation plans.
- Do not introduce a Symphony server or a new orchestration service unless the user explicitly asks for that architecture change.

When multiple agents work at once, each agent must declare:

- Target service or document.
- Files it expects to touch.
- Contracts it depends on.
- Verification command.

Read `references/multi-agent-collaboration.md` before multi-agent planning, architecture review, or parallel development.

Use `references/agent-role-prompts.md` when assigning work to other AI tools or human developers.

Use `references/development-workflow.md` before starting a new development slice or issue-style task.

For parallel execution, the lead agent must create a Parallel Work Order first, assign disjoint file ownership, freeze shared contracts, then dispatch specialist agents. The lead agent owns final integration, verification, docs, and archive updates.

## Before Completion

Always report:

- Files changed.
- Service boundary affected.
- Verification run.
- Known blockers.
- Whether docs and archive snapshots were updated.
