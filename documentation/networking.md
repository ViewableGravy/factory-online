# Networking Layout

The canonical networking architecture note now lives in the Obsidian vault.

Vault note:

- `/mnt/c/Users/lleyt/factory-vault/knowledge/architecture/Java Client Server And Network Adapter Layout.md`

Short summary:

- Keep `java/apps/client` and `java/apps/server` as executable runtimes.
- Do not keep networking implementation code in the repo yet.
- Document requirements first, then implement the transport, protocol, and fault-injection pieces manually later.
- When implementation starts, prefer one physical module like `java/libs/networking/` rather than splitting the code immediately across multiple networking modules.
- Keep the Java package namespace explicit, even if the physical module path is simplified.
- The current terminal-input authoritative tick flow is documented in the `Terminal Input To Authoritative Tick Flow` section of the vault note above.
