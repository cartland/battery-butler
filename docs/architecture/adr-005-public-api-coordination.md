# ADR-005: Coordinate the Shared Client/Server API in the Public Repo

## Status
Accepted

## Context
The Battery Butler client is open source (this repository). The production
server is closed source and lives in a separate private repository. The two
share a wire API, so their maintainers must agree on a contract — endpoints,
request/response shapes, error semantics, versioning — and will do so repeatedly
as the API grows.

Coordinating across an open/closed boundary carries a specific risk: a
contributor who can see the private server internals can accidentally leak them
(a secret endpoint, an internal field name, an unreleased capability, an infra
detail) into the public repository — in prose or, more dangerously, in code
where the leak is hard to spot.

We need a way to collaborate on the public API that keeps the server's secrets
safe while still letting server-side maintainers participate fully in the design.

## Decision
We coordinate the shared API **in the public repository, as documents**, and we
enforce an **information barrier** on who may edit what.

1. **Single shared surface.** Proposals and decisions for the shared API live in
   this public repo under [`docs/api-proposals/`](../api-proposals/README.md).
   Each shared-API change is one numbered proposal document, reviewed by both
   sides on a public PR.

2. **Contribution boundary.** The deciding question for any session — human or
   agent — is whether it has access to the private server source code:
   - A session **with** private-server access may edit **documentation only**
     (proposals and decisions), never application/build/config **code** anywhere
     in this repo (including the in-repo `server/` reference module).
   - A session **without** private-server access may edit **code** (and docs)
     freely.

   The test is *access*, not job title: a server maintainer working in a session
   that has no private-server code checked out is, for that session, a
   client-side contributor.

3. **Small, text-only audit surface.** Keeping server-side contributions to prose
   means the leak audit is a tractable, text-only review that one reviewer can do
   thoroughly. A **secret-safety review** by a server-side reviewer gates every
   proposal merge (checklist in the `docs/api-proposals/` README).

## Consequences

### Positive
- The API contract is designed and recorded in the open, versioned alongside the
  client that consumes it.
- Server secrets are protected structurally: client-code sessions can't leak what
  they never saw, and server-side sessions are confined to an auditable text
  surface.
- Server-side maintainers remain full participants in API *design* and
  *decisions* — only the public *code* is off-limits to them.

### Negative
- Server-side maintainers cannot directly land client code; a client-side session
  must implement the client half.
- Requires discipline: sessions must correctly self-classify by private-server
  access and stay on the correct side of the boundary.
- Adds a review step (secret-safety review) before proposal merges.

## Notes
- Operational details — the proposal lifecycle, numbering, template, and the
  secret-safety checklist — live in
  [`docs/api-proposals/README.md`](../api-proposals/README.md). Update that doc
  as the process evolves; update this ADR only if the underlying decision changes.
- The contributor-facing summary is in [`CONTRIBUTING.md`](../../CONTRIBUTING.md);
  the agent-facing rule is Critical Rule #7 in `.agent/AGENTS.md`.
