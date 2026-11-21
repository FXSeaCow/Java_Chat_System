# Java Chat System

The repository uses a Maven multi-module build. The `common` module contains
the protocol types, DTOs, and models shared by the server and clients. Its Java
package remains `com.javachat.shared` for compatibility with existing imports.
The `server` module contains the local HTTP API and depends on
`common`. The `admin-client` module is a frontend source skeleton. The
`user-client` module contains the chat UI: the main window and conversation
list (JC-25), the chat frame (JC-26), history search (JC-27), the AI reply
suggestion panel (JC-28), the friend list screen (JC-30), the friend request
screen (JC-31), the online friends screen (JC-32), and the account profile
screen (JC-22).

Build all modules from the repository root with `mvn verify`.

Copy `config.properties.example` to `config.properties` and enter the host,
port, username, and password shown in Supabase Dashboard > Connect. Use the
Direct connection or Session pooler values; the JDBC URL must begin with
`jdbc:postgresql://`. The file is ignored by Git. Run `database/schema.sql`
in the Supabase SQL Editor first, and optionally run `database/seed.sql` on a
new empty database for demo data.

The server module loads database settings from `config.properties` in the
working directory. Alternatively, set `JAVA_CHAT_CONFIG` to another file path
or provide `JAVA_CHAT_DATABASE_URL`, `JAVA_CHAT_DATABASE_USERNAME`, and
`JAVA_CHAT_DATABASE_PASSWORD` as environment variables. The URL, username,
and password environment variables override the file. To check connectivity:

```powershell
mvn -pl server -am install -DskipTests
mvn -pl server exec:java -Dexec.mainClass=com.javachat.server.database.DatabaseCheck
```

To load Supabase data in the desktop app, start the server and client in
separate PowerShell terminals from the repository root:

```powershell
.\run-server.ps1
```

```powershell
.\run-client.ps1
```

`run-server.ps1` expects pgJDBC 42.7.14 in the local Maven cache. Run
`mvn -pl server -am install -DskipTests` once if the JAR is missing.
The client connects to `http://127.0.0.1:8080` by default. If you change
`server.port`, set `JAVA_CHAT_API_URL` to the matching loopback URL before
starting the client. The server reads and writes conversations, messages,
friend requests, and groups in PostgreSQL. It binds only to loopback and uses
`server.user_id=1` as a development account; account login is not implemented.
Presence and AI suggestions are still unavailable in live mode.

To preview the chat UI with sample data, run
`com.javachat.user.UserClientApp` from the `user-client` module with `--demo`.
On Windows PowerShell, you can build and launch that preview directly from the
repository root with `powershell -ExecutionPolicy Bypass -File .\run-demo.ps1`.
The preview uses `MockChatStore`, an in-memory snapshot of the users,
friendships, conversations, group members, and messages in `database/seed.sql`.
It does not query Supabase; preview changes disappear when the app closes.
Without `--demo`, the desktop loads a live snapshot from the local server
and reports a connection error if that server or the database is unavailable.

Use the **Bạn bè** navigation button to view friends. After a
`FRIEND_LIST_RESPONSE`, pass the DTO to `MainFrame.setFriendList(...)` on the
Swing event dispatch thread. `setOpenFriendConversationListener(...)` receives
the selected `User` when **Nhắn tin** is clicked or a friend is double-clicked;
the application layer can then find or create the direct conversation. Demo mode
provides two sample friends and opens their sample conversations.

Use **Lời mời kết bạn** to view incoming and outgoing requests from the same
`FriendListResponse`. Call `MainFrame.setFriendRequestUsers(...)` if user
profiles are available for the request rows; otherwise the screen shows user
IDs. `setRespondFriendRequestListener(...)` supplies a `RespondRequest` when an
incoming request is accepted or rejected. Refresh the screen with
`setFriendList(...)` after the server response, or call
`setFriendRequestResponseError(friendshipId)` to allow a retry after failure.
The seed account (user ID 1) has no pending incoming or outgoing requests,
so those lists start empty in demo mode.

Use **Đang online** to see friends who are currently online. Pass an initial
set of online user IDs with `MainFrame.setOnlineUserIds(...)` if available, then
forward each `USER_PRESENCE_CHANGED_EVENT` payload to
`MainFrame.applyPresence(...)` on the Swing event dispatch thread. **Nhắn tin**
uses the same listener as the friends screen. Presence is transient and has no
table in the current schema, so demo mode simulates user ID 2 changing status
every 10 seconds.

Use **Tài khoản** to view and edit the signed-in user's profile. Pass the user
with `MainFrame.setCurrentUser(...)`; saving the form calls
`setProfileUpdateListener(...)` with a `ProfileUpdateRequest`. Report the result
with `setProfileUpdateSucceeded(updatedUser)` or `setProfileUpdateFailed(message)`
on the Swing event dispatch thread. Demo mode saves changes in memory; live mode
reports that the server does not support profile updates yet.

AI usage for each Jira ticket is logged in `docs/ai-log/`.
