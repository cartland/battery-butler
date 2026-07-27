# API Proposal NNNN: <Short Title>

<!--
  Copy this file to `NNNN-short-slug.md` (next unused 4-digit number) and fill it
  in. Delete these HTML comments as you go. See README.md for the full workflow
  and the secret-safety review server-side reviewers must run before merge.
-->

| Field | Value |
|-------|-------|
| **Status** | Draft \| Under Review \| Accepted \| Implemented \| Rejected \| Superseded by NNNN |
| **Authors** | Name (client-side \| server-side) |
| **Created** | YYYY-MM-DD |
| **Updated** | YYYY-MM-DD |

## Summary

One paragraph: what shared API is being added or changed, and for whom.

## Motivation

Why does the client and server need this? What can't be done today? What user-
facing capability or fix does it unblock?

## Proposed API

The wire contract, in enough detail that the client and server can implement
independently and interoperate on the first try.

### Endpoints / messages

| Method & path (or message) | Purpose |
|---|---|
| `GET /v1/...` | … |

### Request

```
<shape: fields, types, required/optional, example>
```

### Response

```
<shape: fields, types, example>
```

### Errors

| Condition | Status / code | Client behavior |
|---|---|---|
| … | … | … |

### Versioning & compatibility

How this fits the existing contract. Is it additive? Does it change existing
behavior? How do old clients and old servers behave against the new counterpart?

## Backward compatibility & migration

What happens during the window where one side has shipped and the other hasn't.
Roll-out order, feature flags, graceful degradation.

## Security & privacy

Auth requirements, data sensitivity, rate limits, abuse considerations for this
endpoint. (This is about the *public* contract — do not put private server
internals here.)

### Secret-safety review

Filled in by a server-side reviewer before merge (see README checklist).

- [ ] Reviewed — publishes nothing the private server must keep secret.
- Reviewer / date: …

## Client implementation plan

Code changes land in this (public) repo via normal PRs, made by client-side
sessions.

- [ ] …

## Server implementation notes

Described here at the contract level; the actual code lives in the private
server repo. Note anything the client must know (e.g. rollout timing) — but no
private internals.

- …

## Open questions

- …

## Decision log

- **YYYY-MM-DD** — Drafted.
- **YYYY-MM-DD** — Secret-safety review passed by <reviewer>.
- **YYYY-MM-DD** — Accepted. Rationale: …
