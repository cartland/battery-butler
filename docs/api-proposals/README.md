# Shared Client/Server API Proposals

This directory is where the **Battery Butler client** and the **Battery Butler
server** coordinate the API contract they share.

The client is **open source** (this repository). The production server is
**closed source** and lives in a separate private repository. The two still need
to agree on the wire contract between them — endpoints, request/response shapes,
error semantics, versioning. This directory is the neutral, public place where
that agreement is proposed, discussed, and recorded, so that neither side has to
reveal the other's private code to reach a decision.

> **The one-sentence version:** we design the shared API *in the open, as
> documents*, in this public repo. People and agents who can see the private
> server code contribute here **by editing documentation only**; people and
> agents who edit the client's **code** must be working *without* access to the
> private server code.

## Why this exists

Coordinating an API across an open/closed boundary has a specific risk: the side
that knows the private server internals can accidentally leak them — a secret
endpoint, an internal field name, an unreleased capability, an infra detail —
into the public repository, either in prose or in code.

We manage that risk with an **information barrier** (a "Chinese wall"):

- The public repo is the **only** shared surface. Proposals and decisions live
  here as plain documents, reviewable by anyone.
- The audit surface is kept **small and text-only** so that server-side
  contributors can realistically read every word they publish and confirm no
  secret escaped. Auditing a prose diff is tractable; auditing a code change for
  subtle leakage is not.
- The barrier is enforced by **who does what**, not by trust alone — see the
  contribution boundary below.

## The contribution boundary

The deciding question for any session — human or agent — is:

> **Does this session have access to the private server source code?**

| If the session… | It MAY edit… | It MUST NOT edit… | Why |
|---|---|---|---|
| **Has** access to the private server code (server-side) | Documentation only — proposals and decisions in this repo (primarily `docs/api-proposals/` and related docs) | Application, build, or configuration **code** anywhere in this repo (including the in-repo `server/` reference module) | Confining server-side work to prose keeps the leak-audit surface small and text-only. A code diff is too hard to audit for subtle secret leakage. |
| **Does not** have access to the private server code (client-side) | Client **code** and documentation freely | — | A session that never saw the server's secrets structurally cannot leak them into public code. |

Notes:

- **The test is access, not job title.** A server engineer working in a session
  that has *no* private-server code checked out is, for that session, a
  client-side contributor and may edit code. The barrier is about what the
  session can see, not who the person is.
- **"Documentation only" includes decisions.** Server-side contributors are
  expected to write proposals, review them, record decisions, and update these
  docs. That is the whole point — they are full participants in the *design*,
  just not in the public *code*.
- **When in doubt, stay on the docs side of the line.** If a session might have
  private-server context, treat it as server-side and restrict to documentation.

## How a proposal flows

Each shared-API change is one numbered proposal document in this directory.

1. **Draft.** Anyone (client-side or server-side) copies
   [`TEMPLATE.md`](./TEMPLATE.md) to `NNNN-short-slug.md`, where `NNNN` is the
   next unused 4-digit number (`0001`, `0002`, …). Fill in the contract you want
   the client and server to share. Open a PR.
2. **Under review.** Both sides comment on the PR / the doc. Server-side
   reviewers additionally run the **secret-safety review** (below) before
   anything merges. Iterate in the document.
3. **Accepted.** When both sides agree, set the status to `Accepted` and record
   the decision in the proposal's Decision Log. The merged document is the
   contract of record.
4. **Implemented.** Client code lands via normal PRs in this repo (by
   client-side sessions). The private server implements its half in its own
   repo. When both halves ship, set the status to `Implemented`.
5. **Rejected / Superseded.** Proposals that don't proceed are kept for the
   record with status `Rejected`, or `Superseded by NNNN` when a later proposal
   replaces them. We don't delete proposals — the history is the value.

### Statuses

`Draft` → `Under Review` → `Accepted` → `Implemented`, with `Rejected` and
`Superseded by NNNN` as terminal side branches.

## Secret-safety review (server-side reviewers)

Before a proposal PR merges, a server-side reviewer confirms the document
publishes **nothing** the private server needs to keep secret. Walk this
checklist and record the result in the proposal's Decision Log:

- [ ] No secret or internal-only endpoints, hostnames, or paths.
- [ ] No credentials, tokens, keys, or secret-management details.
- [ ] No internal field names, schemas, or data-model details that aren't part
      of the public contract.
- [ ] No unreleased or roadmap capabilities that shouldn't be public yet.
- [ ] No infrastructure, deployment, or vendor details.
- [ ] The document describes **only** the wire contract the client legitimately
      needs to know.

If any item fails, the fix is to edit the prose — not to move the leak into
code. Because the whole proposal is text, this audit is something one reviewer
can do thoroughly in one sitting; that is exactly why the boundary keeps
server-side contributions to documents.

## Index of proposals

| # | Title | Status | Updated |
|---|-------|--------|---------|
| _none yet_ | Copy [`TEMPLATE.md`](./TEMPLATE.md) to `0001-<slug>.md` to start the first one. | — | — |

## See also

- [`TEMPLATE.md`](./TEMPLATE.md) — the per-proposal template.
- [`../architecture/adr-005-public-api-coordination.md`](../architecture/adr-005-public-api-coordination.md)
  — the architecture decision record that established this model.
- [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md) — contributor-facing summary of
  the boundary.
