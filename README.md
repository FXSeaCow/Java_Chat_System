# Java Chat System

The repository uses a Maven multi-module build. The `common` module contains
the protocol types, DTOs, and models shared by the server and clients. Its Java
package remains `com.javachat.shared` for compatibility with existing imports.
The `server` module contains the backend source skeleton and depends on
`common`. The `admin-client` module is a frontend source skeleton. The
`user-client` module contains the chat UI: the main window and conversation
list (JC-25), the chat frame (JC-26), history search (JC-27), and the AI reply
suggestion panel (JC-28). Network integration is still pending.

Build all modules from the repository root with `mvn verify`.

Copy `config.properties.example` to `config.properties` for local settings. The
example contains no credentials. Configuration loading will be added when the
networked application code is implemented.

To preview the chat UI with sample data, run
`com.javachat.user.UserClientApp` from the `user-client` module with `--demo`.
Without that argument the window displays its empty state. When a
`CONVERSATION_LIST_RESPONSE` arrives, pass `response.conversations()` to
`MainFrame.setConversations(...)` on the Swing event dispatch thread. Use
`setOpenConversationListener(...)` to connect selection to the future message
screen.

AI usage for each Jira ticket is logged in `docs/ai-log/`.
