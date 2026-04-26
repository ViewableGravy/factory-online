# Client–Server Split — POC TODOs

The following TODOs are scoped for an initial proof-of-concept split where we keep the same authoritative tick rate and avoid security, ops, tests, migration, tooling, backpressure, determinism work, prediction, reconnects, and protocol versioning.

## Remaining TODOs

1. Define `Transport` abstraction
   - Add a `Transport` interface that decouples in-process (`LocalTransportHub`) from network transports (TCP/UDP/WebSocket). Keep APIs for send/receive, lifecycle (connect/close), and per-client addressing.

4. Define entity ownership & authority rules
   - Make server authoritative for creation/destruction and define rules for ownership transfers and client-side expectations.

5. Decide concurrency & server threading model
   - Choose a model (e.g., single-threaded tick loop + IO worker threads). Document locking/ownership expectations for shared state.

---

## Next steps (suggested)
- Confirm you want these prioritized; I can pick the top 1–2 items and start implementing them (e.g., `Transport` abstraction and shared types).

## Completed (recent)
- Sessions: server now manages `Session` objects and enforces acceptance policy (only `client-1`).
- Ack/Rej: server sends Ack and Rejection messages to clients for join and input paths.
- Buffering & server-side validation: inputs are validated and buffered per-target tick before application.
- `/server` command: admin `/server <cmd>` runs on server immediately and still broadcasts updates to clients.


