# Client-Server Split - Current Status

The current proof-of-concept now runs as true separate server and client applications.

- `java/run-server.sh` starts the single authoritative server.
- `java/run-client.sh` starts a client that connects over TCP and subscribes to a random server simulation.
- The transport seam, join/ack flow, simulation updates, periodic tick sync, and conservative client pacing remain in place across the split runtime.


