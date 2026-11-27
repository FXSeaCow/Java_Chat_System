package com.javachat.user;

import com.javachat.shared.dto.AiDtos;
import com.javachat.shared.dto.AuthDtos.PresenceChangedEvent;
import com.javachat.shared.dto.FriendDtos.FriendRequestDecision;
import com.javachat.shared.model.PresenceStatus;
import com.javachat.user.mock.MockChatStore;
import com.javachat.user.net.LiveApiClient;
import com.javachat.user.net.PresenceSocketClient;
import com.javachat.user.ui.chat.MainFrame;
import com.javachat.user.ui.auth.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

public final class UserClientApp {
    private static final String LAUNCH_ID = java.util.UUID.randomUUID().toString();
    private static final String OPENED_AT = Instant.now().toString();
    private UserClientApp() { }

    public static void main(String[] args) {
        if (List.of(args).contains("--preview-otp")) {
            SwingUtilities.invokeLater(LoginFrame::previewOtp);
            return;
        }
        boolean demo = List.of(args).contains("--demo");
        if (!demo) CompletableFuture.runAsync(() -> {
            try {
                new LiveApiClient().post("/api/app-launch", launchData());
            } catch (java.io.IOException error) {
                System.err.println("Không ghi được lần mở ứng dụng.");
            }
        });
        SwingUtilities.invokeLater(() -> showLogin(demo));
    }

    private static Map<String, Object> launchData() {
        return Map.of("launchId", LAUNCH_ID, "openedAt", OPENED_AT);
    }

    private static void showLogin(boolean demo) {
        AtomicReference<LiveApiClient> authenticatedApi = new AtomicReference<>();
        LoginFrame login = new LoginFrame(() -> {
            MainFrame frame = new MainFrame();
            LiveApiClient api = authenticatedApi.get();
            frame.setLogoutListener(() -> logout(frame, api, demo));
            frame.setPasswordChangeListener(request -> changePassword(frame, api, request));
            frame.setVisible(true);
            if (demo) {
                bind(frame, new MockChatStore(), null);
            } else {
                loadLive(frame, api);
            }
        }, demo ? null : (loginName, password) -> {
            LiveApiClient api = new LiveApiClient();
            api.login(loginName, password);
            authenticatedApi.set(api);
            CompletableFuture.runAsync(() -> {
                try {
                    api.post("/api/auth/app-open", launchData());
                } catch (java.io.IOException error) {
                    System.err.println("Không ghi được lần mở ứng dụng.");
                }
            });
        }, demo ? null : email -> {
            new LiveApiClient().post("/api/auth/register/send-otp", Map.of("email", email));
        }, demo ? null : (displayName, email, password, otp) -> {
            var result = new LiveApiClient().post("/api/auth/register", Map.of(
                    "displayName", displayName, "email", email, "password", password, "otp", otp));
            return ((Number) result.get("userId")).longValue();
        }, demo ? null : (email, otp) -> {
            var api = new LiveApiClient();
            if (otp == null) {
                api.post("/api/auth/reset-password/send-otp", Map.of("email", email));
            } else {
                api.post("/api/auth/reset-password", Map.of("email", email, "otp", otp));
            }
        });
        login.setVisible(true);
    }

    private static void logout(MainFrame frame, LiveApiClient api, boolean demo) {
        if (api == null) {
            frame.dispose();
            showLogin(demo);
            return;
        }
        frame.setLogoutPending(true);
        CompletableFuture.runAsync(() -> {
            try {
                api.logout();
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            frame.dispose();
            showLogin(false);
            if (error != null) {
                showError(null, "Đã đăng xuất trên máy này, nhưng server chưa xác nhận hủy phiên.", error);
            }
        }));
    }

    private static void changePassword(MainFrame frame, LiveApiClient api,
                                       com.javachat.shared.dto.AuthDtos.ChangePasswordRequest request) {
        if (api == null) {
            frame.setPasswordChangeFailed("Chế độ demo không đổi mật khẩu. Hãy đăng nhập ở chế độ live.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                api.changePassword(request.currentPassword(), request.newPassword());
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (!frame.isDisplayable()) return;
            if (error != null) {
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                frame.setPasswordChangeFailed(cause.getMessage());
                return;
            }
            frame.setPasswordChangeSucceeded();
            frame.dispose();
            showLogin(false);
            JOptionPane.showMessageDialog(null,
                    "Đã đổi mật khẩu. Vui lòng đăng nhập lại bằng mật khẩu mới.",
                    "E-Chat", JOptionPane.INFORMATION_MESSAGE);
        }));
    }

    private static void loadLive(MainFrame frame, LiveApiClient api) {
        try {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return MockChatStore.fromSnapshot(api.snapshot());
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }).whenComplete((store, error) -> SwingUtilities.invokeLater(() -> {
                if (!frame.isDisplayable()) return;
                if (error != null) showError(frame, "Không tải được dữ liệu từ server. "
                        + "Hãy chạy ServerApp và kiểm tra schema Supabase.", error);
                else bind(frame, store, api);
            }));
        } catch (Exception error) {
            showError(frame, "Cấu hình API không hợp lệ.", error);
        }
    }

    private static void bind(MainFrame frame, MockChatStore initial, LiveApiClient api) {
        AtomicReference<MockChatStore> current = new AtomicReference<>(initial);
        frame.setCurrentUserId(initial.currentUserId());
        frame.setGroupSelectedListener(id ->
                frame.setGroupMembers(current.get().groupMembers(id)));
        frame.setOpenFriendConversationListener(friend ->
                frame.openConversation(current.get().directConversationWith(friend.id())));
        frame.setOpenConversationListener(item -> frame.setMessages(item.conversation().id(),
                current.get().messages(item.conversation().id())));

        frame.setRespondFriendRequestListener(request -> {
            boolean accept = request.decision() == FriendRequestDecision.ACCEPT;
            if (api == null) {
                current.get().respondToFriendRequest(request.friendshipId(), accept);
                refresh(frame, current.get());
            } else runRemote(frame, api, current, () -> {
                api.post("/api/friend-requests/respond", Map.of(
                        "friendshipId", request.friendshipId(), "accept", accept));
                return null;
            });
        });
        frame.setAddFriendListener(request -> {
            if (api == null) {
                current.get().sendFriendRequest(request.addresseeId());
                refresh(frame, current.get());
            } else runRemote(frame, api, current, () -> {
                api.post("/api/friend-requests", Map.of("addresseeId", request.addresseeId()));
                return null;
            });
        });
        frame.setSendMessageListener(request -> {
            if (api == null) {
                frame.appendMessage(current.get().send(request));
                frame.setConversations(current.get().conversationSummaries());
            } else runRemote(frame, api, current, () -> {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("conversationId", request.conversationId());
                body.put("replyToId", request.replyToId());
                body.put("content", request.content());
                api.post("/api/messages", body);
                return request.conversationId();
            });
        });
        frame.setCreateGroupListener(request -> {
            if (api == null) {
                var created = current.get().createGroup(request);
                refresh(frame, current.get());
                frame.openConversation(created);
            } else runRemote(frame, api, current, () -> {
                var result = api.post("/api/groups", Map.of(
                        "name", request.name(), "description", request.description(),
                        "memberIds", request.initialMemberIds()));
                return ((Number) result.get("id")).longValue();
            });
        });
        frame.setUpdateGroupListener(request -> {
            if (api == null) {
                current.get().updateGroup(request);
                refresh(frame, current.get());
            } else runRemote(frame, api, current, () -> {
                api.post("/api/groups/update", Map.of(
                        "conversationId", request.conversationId(), "name", request.name(),
                        "description", request.description()));
                return null;
            });
        });
        frame.setRemoveGroupMemberListener(request -> {
            if (api == null) {
                current.get().removeGroupMember(request.conversationId(), request.userId());
                frame.setGroupMembers(current.get().groupMembers(request.conversationId()));
            } else runRemote(frame, api, current, () -> {
                api.post("/api/groups/remove-member", Map.of(
                        "conversationId", request.conversationId(), "userId", request.userId()));
                return null;
            });
        });
        frame.setMemberRoleListener(request -> {
            if (api == null) {
                current.get().changeGroupRole(request.conversationId(), request.userId(), request.role());
                frame.setGroupMembers(current.get().groupMembers(request.conversationId()));
            } else runRemote(frame, api, current, () -> {
                api.post("/api/groups/role", Map.of(
                        "conversationId", request.conversationId(), "userId", request.userId(),
                        "role", request.role().name()));
                return null;
            });
        });
        frame.setProfileUpdateListener(request -> {
            if (api == null) {
                frame.setProfileUpdateSucceeded(current.get().updateProfile(request));
                refresh(frame, current.get());
            } else {
                CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("displayName", request.displayName());
                        body.put("avatarUrl", request.avatarUrl());
                        body.put("address", request.address());
                        body.put("dateOfBirth", request.dateOfBirth() == null ? null : request.dateOfBirth().toString());
                        body.put("gender", request.gender() == null ? null : request.gender().name());
                        api.post("/api/profile/update", body);
                    } catch (java.io.IOException error) {
                        throw new CompletionException(error);
                    }
                }).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
                    if (!frame.isDisplayable()) return;
                    if (error != null) {
                        frame.setProfileUpdateFailed("Chưa thể lưu hồ sơ. Vui lòng thử lại.");
                        return;
                    }
                    frame.setProfileUpdateSucceeded(current.get().updateProfile(request));
                    refresh(frame, current.get());
                }));
            }
        });
        if (api == null) {
            frame.setAiSuggestionListener(request -> frame.setAiSuggestionResult(request.conversationId(),
                    new AiDtos.ChatResponse(1L, "Mình đang làm phần JDBC, lát nữa gửi bạn xem nhé.",
                            "demo-chat-model", 18, 34)));
            frame.setOnlineUserIds(List.of(2L));
            boolean[] baoOnline = {true};
            Timer presenceTimer = new Timer(10_000, event -> {
                baoOnline[0] = !baoOnline[0];
                frame.applyPresence(new PresenceChangedEvent(2L,
                        baoOnline[0] ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE, Instant.now()));
            });
            presenceTimer.start();
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent event) { presenceTimer.stop(); }
            });
        } else {
            frame.setAiSuggestionListener(request -> frame.setAiSuggestionError(request.conversationId(),
                    "Gợi ý AI chưa được kết nối với server."));
            PresenceSocketClient events = api.listenForPresence(update ->
                    SwingUtilities.invokeLater(() -> {
                        if (frame.isDisplayable()) frame.applyPresence(update);
                    }));
            Timer presenceTimer = new Timer(15_000, event -> CompletableFuture.supplyAsync(() -> {
                try {
                    return api.onlineFriendIds();
                } catch (Exception error) {
                    return null;
                }
            }).thenAccept(ids -> SwingUtilities.invokeLater(() -> {
                if (frame.isDisplayable() && ids != null) frame.setOnlineUserIds(ids);
            })));
            presenceTimer.start();
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent event) {
                    presenceTimer.stop();
                    events.close();
                }
            });
        }
        refresh(frame, initial);
    }

    private static void refresh(MainFrame frame, MockChatStore store) {
        frame.setCurrentUser(store.currentUser());
        frame.setPeople(store.people());
        frame.setFriendIds(store.friendIds());
        frame.setPendingFriendIds(store.pendingFriendIds());
        frame.setFriendList(store.friendList());
        frame.setFriendRequestUsers(store.requestUsers());
        frame.setGroupCandidates(store.people());
        frame.setGroupUsers(store.allUsers());
        frame.setGroups(store.myGroups());
        frame.setGroupMembers(store.myGroups().isEmpty() ? List.of()
                : store.groupMembers(store.myGroups().get(0).conversationId()));
        frame.setConversations(store.conversationSummaries());
    }

    private static void runRemote(MainFrame frame, LiveApiClient api,
                                  AtomicReference<MockChatStore> current, RemoteAction action) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Long conversationId = action.run();
                return new RemoteResult(conversationId, MockChatStore.fromSnapshot(api.snapshot()));
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }).whenComplete((result, error) -> SwingUtilities.invokeLater(() -> {
            if (!frame.isDisplayable()) return;
            if (error != null) {
                showError(frame, "Không lưu được thay đổi lên Supabase.", error);
                return;
            }
            current.set(result.store());
            refresh(frame, result.store());
            if (result.conversationId() != null) {
                result.store().conversationSummaries().stream()
                        .filter(item -> item.conversation().id() == result.conversationId())
                        .findFirst().ifPresent(frame::openConversation);
            }
        }));
    }

    private static void showError(MainFrame frame, String message, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        JOptionPane.showMessageDialog(frame, message + "\n" + cause.getMessage(),
                "E-Chat", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface RemoteAction {
        Long run() throws Exception;
    }

    private record RemoteResult(Long conversationId, MockChatStore store) { }
}
