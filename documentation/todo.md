# Client-Server Split - Remaining Step

The current proof-of-concept has the transport seam, periodic tick sync, and conservative client pacing in place while the runtime still runs inside the combined manual harness.

## Remaining work

1. Split the current combined application into true separate server and client applications.
   - Keep the current transport and protocol behavior intact while moving off the shared in-process harness.
   - Validate that join, ack/rejection, simulation updates, and tick-sync behavior still match once the applications run independently.


