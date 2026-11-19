# Java Chat System

The repository uses a Maven multi-module build. The `common` module contains
the protocol types, DTOs, and models shared by the server and clients. Its Java
package remains `com.javachat.shared` for compatibility with existing imports.
The `server` module contains the backend source skeleton and depends on
`common`. The `admin-client` module is a frontend source skeleton. The
`user-client` module contains the chat UI: the main window and conversation
list (JC-25), the chat frame (JC-26), history search (JC-27), the AI reply
suggestion panel (JC-28), the friend list screen (JC-30), the friend request
screen (JC-31), and the online friends screen (JC-32). Network integration is still pending.

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
Demo mode includes one incoming and one outgoing request.

Use **Đang online** to see friends who are currently online. Pass an initial
set of online user IDs with `MainFrame.setOnlineUserIds(...)` if available, then
forward each `USER_PRESENCE_CHANGED_EVENT` payload to
`MainFrame.applyPresence(...)` on the Swing event dispatch thread. **Nhắn tin**
uses the same listener as the friends screen. In demo mode one friend is online
and another toggles online/offline every 10 seconds.

AI usage for each Jira ticket is logged in `docs/ai-log/`.
