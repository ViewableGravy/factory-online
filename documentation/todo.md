# Client–Server Split — POC TODOs

The following TODOs are scoped for an initial proof-of-concept split where we keep the same authoritative tick rate and avoid security, ops, tests, migration, tooling, backpressure, determinism work, prediction, reconnects, and protocol versioning.

## Remaining TODOs

1. Define `Transport` abstraction
   - Add a `Transport` interface that decouples in-process (`LocalTransportHub`) from network transports (TCP/UDP/WebSocket). Keep APIs for send/receive, lifecycle (connect/close), and per-client addressing.

2. Implement serialization layer
   - Choose and implement a stable serializer (e.g., Protobuf or compact JSON). Ensure deterministic encoding for snapshots and commands.

3. Define message framing & channels
   - Design framing for stream transports and logical channels (control, reliable, best-effort). Identify message boundaries and sizes.

4. Implement sequencing & ACK tracking
   - Add sequence numbers and optional ACK/receipt tracking for critical messages to enforce ordering where needed.

5. Support multiple clients
   - Server must accept and manage multiple simultaneous clients; track `ClientId` and route messages per-client.

6. Add session management
   - Implement `Session` objects mapping network connections to in-game player entities. Handle connect/start/stop lifecycle (explicit reconnect/resume is out of scope).

7. Implement authoritative server tick & `Tick` message
   - Server remains authoritative for ticks. Create a minimal `Tick` message that the server sends each tick; clients only advance simulation when they receive it.

8. Add input buffering & server-side validation
    - Serialize client inputs as per-tick command batches, buffer them server-side for the tick they apply to, and validate on receipt.

9. Define entity ownership & authority rules
    - Make server authoritative for creation/destruction and define rules for ownership transfers and client-side expectations.

10. Decide concurrency & server threading model
    - Choose a model (e.g., single-threaded tick loop + IO worker threads). Document locking/ownership expectations for shared state.

---

## Next steps (suggested)
- Confirm you want these prioritized; I can pick the top 1–2 items and start implementing them (e.g., `Transport` abstraction and shared types).


