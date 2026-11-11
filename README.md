# Java Chat System

The repository uses a Maven multi-module build. The `common` module contains
the protocol types, DTOs, and models shared by the server and clients. Its Java
package remains `com.javachat.shared` for compatibility with existing imports.
The `server` module contains the backend source skeleton and depends on
`common`. The `user-client` and `admin-client` modules are frontend source
skeletons.

Build all modules from the repository root with `mvn verify`.

Copy `config.properties.example` to `config.properties` for local settings. The
example contains no credentials. Configuration loading will be added when the
networked application code is implemented.

AI usage for each Jira ticket is logged in `docs/ai-log/`.
