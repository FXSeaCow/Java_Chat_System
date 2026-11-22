# Java Chat System

The repository uses a Maven multi-module build. The `common` module contains
the protocol types, DTOs, and models shared by the server and clients. Its Java
package remains `com.javachat.shared` for compatibility with existing imports.
The `server` module contains the local HTTP API and depends on
`common`. The `admin-client` module contains the Swing admin dashboard with
screens for login history, groups, spam reports, user management, new users,
active users, and registration statistics. The `user-client` module contains the chat UI: the
main window and conversation list (JC-25), the chat frame (JC-26), history
search (JC-27), the AI reply
suggestion panel (JC-28), the friend list screen (JC-30), the friend request
screen (JC-31), the online friends screen (JC-32), and the account profile
screen (JC-22).

Build all modules from the repository root with `mvn verify`.

To open the live admin dashboard on Windows, start the server first, then run
this in another terminal from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-admin.ps1
```

The admin client signs in through `/api/admin/auth/login` using a `public.users`
account with `role = 'ADMIN'` and `status = 'ACTIVE'`. The server checks the
session role on every admin request. All seven screens load current Supabase data
through `/api/admin/snapshot`; **Làm mới** reloads it. Reviewing or dismissing
a pending spam report updates `public.reports` in Supabase. The **Quản lý người
dùng** page searches and filters `USER` accounts, and can block or unblock a
selected account. Blocking also ends all of that account's active sessions;
unblocking does not restore old sessions. Admin accounts are excluded from
these actions.
The same page has **Thêm người dùng** and **Sửa thông tin** dialogs. Admins can
create a `USER` account with display name, username, email, and password, or
edit an existing user's display name, username, and email. The dialogs validate
input before calling the role-protected server API and refresh the list after
the database write succeeds.
The active-user screen shows accounts with unexpired login sessions; it is not
a real-time presence feed. Admin lists return at most 500 recent rows per page.

The seed admin accounts have legacy password hashes that cannot be used with
the current live login. To create an admin account, register a new account in
the live user client, then promote that specific account in the Supabase SQL
Editor (replace the email with the registered email):

```sql
UPDATE public.users SET role = 'ADMIN'
WHERE email = 'your-registered-email@example.com' AND status = 'ACTIVE';
```

Sign in to the admin client with that email and its password. The promoted
account is reserved for admin sign-in; keep a separate `USER` account for chat.
Database credentials remain on the server and are never stored in the admin
client. `JAVA_CHAT_API_URL` also controls the admin client's loopback API URL.

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
friend requests, and groups in PostgreSQL. It binds only to loopback. Sign-in
checks `public.users` and creates a session in `public.auth_sessions`; each API
request uses the signed-in account's ID.
Presence and AI suggestions are still unavailable in live mode.

In live mode, the registration form calls `POST /api/auth/register` on the local
server and inserts a row into `public.users`. Passwords are stored as PBKDF2
hashes. Registration requires the server and database to be reachable; demo
mode does not save accounts. Log in with the registered email and password.
Use **Tài khoản > Đăng xuất** to return to the sign-in screen. In live mode,
logout marks the current row in `public.auth_sessions` as logged out and clears
the desktop token. In demo mode, it returns to the demo sign-in screen.
Use **Tài khoản > Đổi mật khẩu** to enter the current and new password. The live
server verifies the current password, stores a new PBKDF2 hash, ends all active
sessions for the account, and asks the user to sign in again. Demo mode does
not change passwords.
Existing seed accounts use bcrypt hashes through the Supabase `pgcrypto`
extension. The current seed hash does not match the password stated in
`database/seed.sql`, so use a newly registered account for live sign-in.

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
