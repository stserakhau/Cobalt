package it.auties.whatsapp.socket;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import it.auties.whatsapp.crypto.GroupBuilder;
import it.auties.whatsapp.crypto.GroupCipher;
import it.auties.whatsapp.crypto.SessionBuilder;
import it.auties.whatsapp.crypto.SessionCipher;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.action.ContactAction;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.device.DeviceSentMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.setting.EphemeralSetting;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.sync.HistorySync;
import it.auties.whatsapp.model.sync.PushName;
import it.auties.whatsapp.util.*;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.MESSAGE;
import static it.auties.whatsapp.model.request.Node.*;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.*;

class MessageHandler implements JacksonProvider {
    private static final String SKMSG = "skmsg";
    private static final String PKMSG = "pkmsg";
    private static final String MSG = "msg";

    private final Socket socket;
    private final Cache<ContactJid, GroupMetadata> groupsCache;
    private final Cache<String, List<ContactJid>> devicesCache;
    private final Cache<Chat, Chat> historyCache;
    private final Semaphore lock;

    protected MessageHandler(Socket socket) {
        this.socket = socket;
        this.groupsCache = createCache(Duration.ofMinutes(5), null);
        this.devicesCache = createCache(Duration.ofMinutes(5), null);
        this.historyCache = createCache(Duration.ofMinutes(1), this::onChatReady);
        this.lock = new Semaphore(1);
    }

    private void onChatReady(Chat key, Chat value, RemovalCause cause) {
        if (cause != RemovalCause.EXPIRED) {
            return;
        }

        socket.onChatRecentMessages(key, true);
    }

    private <K, V> Cache<K, V> createCache(Duration duration, RemovalListener<K, V> removalListener) {
        var builder = Caffeine.newBuilder()
                .expireAfterWrite(duration);
        if (removalListener != null) {
            builder.removalListener(removalListener);
        }

        return builder.build();
    }

    @SafeVarargs
    protected final CompletableFuture<Void> encode(MessageInfo info, Entry<String, Object>... attributes) {
        return CompletableFuture.runAsync(this::tryLock)
                .thenComposeAsync(ignored -> isConversation(info) ?
                        encodeConversation(info, attributes) :
                        encodeGroup(info, attributes))
                .thenRunAsync(lock::release)
                .exceptionallyAsync(this::handleMessageFailure);
    }

    @SafeVarargs
    private CompletableFuture<Void> encodeGroup(MessageInfo info, Entry<String, Object>... attributes) {
        var encodedMessage = BytesHelper.messageToBytes(info.message());
        var senderName = new SenderKeyName(info.chatJid()
                .toString(), socket.keys()
                .companion()
                .toSignalAddress());
        var groupBuilder = new GroupBuilder(socket.keys());
        var signalMessage = groupBuilder.createOutgoing(senderName);
        var groupCipher = new GroupCipher(senderName, socket.keys());
        var groupMessage = groupCipher.encrypt(encodedMessage);
        return Optional.ofNullable(groupsCache.getIfPresent(info.chatJid()))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> socket.queryGroupMetadata(info.chatJid()))
                .thenComposeAsync(this::getDevices)
                .thenComposeAsync(allDevices -> createGroupNodes(info, signalMessage, allDevices))
                .thenApplyAsync(preKeys -> createEncodedMessageNode(info, preKeys, groupMessage, attributes))
                .thenComposeAsync(socket::send)
                .thenRunAsync(() -> info.chat()
                        .addMessage(info));
    }

    @SafeVarargs
    private CompletableFuture<Void> encodeConversation(MessageInfo info, Entry<String, Object>... attributes) {
        var encodedMessage = BytesHelper.messageToBytes(info.message());
        var deviceMessage = DeviceSentMessage.newDeviceSentMessage(info.chatJid()
                .toString(), info.message(), null);
        var encodedDeviceMessage = BytesHelper.messageToBytes(deviceMessage);
        var knownDevices = List.of(socket.keys()
                .companion()
                .toUserJid(), info.chatJid());
        return getDevices(knownDevices, true).thenComposeAsync(
                        allDevices -> createConversationNodes(allDevices, encodedMessage, encodedDeviceMessage))
                .thenApplyAsync(sessions -> createEncodedMessageNode(info, sessions, null, attributes))
                .thenComposeAsync(socket::send)
                .thenRunAsync(() -> info.chat()
                        .addMessage(info));
    }

    private void tryLock() {
        try {
            socket.awaitReadyState();
            lock.acquire();
        }catch (InterruptedException exception){
            throw new RuntimeException("Cannot lock", exception);
        }
    }

    private <T> T handleMessageFailure(Throwable throwable) {
        lock.release();
        return socket.errorHandler()
                .handleFailure(MESSAGE, throwable);
    }

    private boolean isConversation(MessageInfo info) {
        return info.chatJid().type() == ContactJid.Type.USER
                || info.chatJid().type() == ContactJid.Type.STATUS;
    }

    @SafeVarargs
    @SneakyThrows
    private Node createEncodedMessageNode(MessageInfo info, List<Node> preKeys, Node descriptor,
                                          Entry<String, Object>... metadata) {
        var body = new ArrayList<Node>();
        if (!preKeys.isEmpty()) {
            body.add(withChildren("participants", preKeys));
        }

        if (descriptor != null) {
            body.add(descriptor);
        }

        if (hasPreKeyMessage(preKeys)) {
            var identity = PROTOBUF.writeValueAsBytes(socket.keys()
                    .companionIdentity());
            body.add(with("device-identity", identity));
        }

        var attributes = Attributes.of(metadata)
                .put("id", info.id())
                .put("type", "text")
                .put("to", info.chatJid())
                .map();
        return withChildren("message", attributes, body);
    }

    private boolean hasPreKeyMessage(List<Node> participants) {
        return participants.stream()
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(node -> node.attributes()
                        .getOptionalString("type"))
                .flatMap(Optional::stream)
                .anyMatch("pkmsg"::equals);
    }

    private CompletableFuture<List<Node>> createConversationNodes(List<ContactJid> contacts, byte[] message,
                                                                  byte[] deviceMessage) {
        var partitioned = contacts.stream()
                .collect(partitioningBy(contact -> Objects.equals(contact.user(), socket.keys()
                        .companion()
                        .user())));
        var companions = querySessions(partitioned.get(true)).thenApplyAsync(
                ignored -> createMessageNodes(partitioned.get(true), deviceMessage));
        var others = querySessions(partitioned.get(false)).thenApplyAsync(
                ignored -> createMessageNodes(partitioned.get(false), message));
        return companions.thenCombineAsync(others, (first, second) -> append(first, second));
    }

    @SneakyThrows
    private CompletableFuture<List<Node>> createGroupNodes(MessageInfo info, byte[] distributionMessage,
                                                           List<ContactJid> participants) {
        Validate.isTrue(info.chat()
                .isGroup(), "Cannot send group message to non-group");

        var missingParticipants = participants.stream()
                .filter(participant -> !info.chat()
                        .participantsPreKeys()
                        .contains(participant))
                .toList();
        if (missingParticipants.isEmpty()) {
            return completedFuture(List.of());
        }

        var whatsappMessage = new SenderKeyDistributionMessage(info.chatJid()
                .toString(), distributionMessage);
        var paddedMessage = BytesHelper.messageToBytes(whatsappMessage);
        return querySessions(missingParticipants).thenApplyAsync(
                        ignored -> createMessageNodes(missingParticipants, paddedMessage))
                .thenApplyAsync(results -> savePreKeys(info.chat(), missingParticipants, results));
    }

    private List<Node> savePreKeys(Chat group, List<ContactJid> missingParticipants, List<Node> results) {
        group.participantsPreKeys()
                .addAll(missingParticipants);
        return results;
    }

    private CompletableFuture<Void> querySessions(List<ContactJid> contacts) {
        var missingSessions = contacts.stream()
                .filter(contact -> !socket.keys()
                        .hasSession(contact.toSignalAddress()))
                .map(contact -> withAttributes("user", of("jid", contact, "reason", "identity")))
                .toList();
        if (missingSessions.isEmpty()) {
            return completedFuture(null);
        }

        return socket.sendQuery("get", "encrypt", withChildren("key", missingSessions))
                .thenAcceptAsync(this::parseSessions);
    }

    private List<Node> createMessageNodes(List<ContactJid> contacts, byte[] message) {
        return contacts.stream()
                .map(contact -> createMessageNode(contact, message))
                .toList();
    }

    private Node createMessageNode(ContactJid contact, byte[] message) {
        var cipher = new SessionCipher(contact.toSignalAddress(), socket.keys());
        var encrypted = cipher.encrypt(message);
        return withChildren("to", of("jid", contact), encrypted);
    }

    private CompletableFuture<List<ContactJid>> getDevices(GroupMetadata metadata) {
        groupsCache.put(metadata.jid(), metadata);
        return getDevices(metadata.participantsJids(), false);
    }

    private CompletableFuture<List<ContactJid>> getDevices(List<ContactJid> contacts, boolean excludeSelf) {
        var partitioned = contacts.stream()
                .collect(partitioningBy(contact -> devicesCache.asMap()
                        .containsKey(contact.user()), toUnmodifiableList()));
        var cached = partitioned.get(true)
                .stream()
                .map(ContactJid::user)
                .map(devicesCache::getIfPresent)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
        var missing = partitioned.get(false);
        if (missing.isEmpty()) {
            return completedFuture(excludeSelf ?
                    append(contacts, cached) :
                    cached);
        }

        return queryDevices(missing, excludeSelf).thenApplyAsync(missingDevices -> excludeSelf ?
                append(contacts, cached, missingDevices) :
                append(cached, missingDevices));
    }

    @SneakyThrows
    private CompletableFuture<List<ContactJid>> queryDevices(List<ContactJid> contacts, boolean excludeSelf) {
        var contactNodes = contacts.stream()
                .map(contact -> withAttributes("user", of("jid", contact)))
                .toList();
        var body = Node.withChildren("usync", of("sid", socket.store()
                        .nextTag(), "mode", "query", "last", "true", "index", "0", "context", "message"),
                withChildren("query", withAttributes("devices", of("version", "2"))),
                withChildren("list", contactNodes));
        return socket.sendQuery("get", "usync", body)
                .thenApplyAsync(result -> parseDevices(result, excludeSelf));
    }

    private List<ContactJid> parseDevices(Node node, boolean excludeSelf) {
        var results = node.children()
                .stream()
                .map(child -> child.findNode("list"))
                .flatMap(Optional::stream)
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(entry -> parseDevice(entry, excludeSelf))
                .flatMap(Collection::stream)
                .toList();
        devicesCache.putAll(results.stream()
                .collect(groupingBy(ContactJid::user)));
        return results;
    }

    private List<ContactJid> parseDevice(Node wrapper, boolean excludeSelf) {
        var jid = wrapper.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid for sync device"));
        return wrapper.findNode("devices")
                .orElseThrow(() -> new NoSuchElementException("Missing devices"))
                .findNode("device-list")
                .orElseThrow(() -> new NoSuchElementException("Missing device list"))
                .children()
                .stream()
                .map(child -> parseDeviceId(child, jid, excludeSelf))
                .flatMap(Optional::stream)
                .map(id -> ContactJid.ofDevice(jid.user(), id))
                .toList();
    }

    private Optional<Integer> parseDeviceId(Node child, ContactJid jid, boolean excludeSelf) {
        var deviceId = child.attributes()
                .getInt("id");
        return child.description()
                .equals("device") && (!excludeSelf || deviceId != 0) && (!jid.user()
                .equals(socket.keys()
                        .companion()
                        .user()) || socket.keys()
                .companion()
                .device() != deviceId) && (deviceId == 0 || child.attributes()
                .hasKey("key-index")) ?
                Optional.of(deviceId) :
                Optional.empty();
    }

    private void parseSessions(Node node) {
        node.findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing list: %s".formatted(node)))
                .findNodes("user")
                .forEach(this::parseSession);
    }

    private void parseSession(Node node) {
        Validate.isTrue(!node.hasNode("error"), "Erroneous session node", SecurityException.class);
        var jid = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid for session"));
        var registrationId = node.findNode("registration")
                .map(id -> BytesHelper.bytesToInt(id.contentAsBytes()
                        .orElseThrow(), 4))
                .orElseThrow(() -> new NoSuchElementException("Missing id"));
        var identity = node.findNode("identity")
                .flatMap(Node::contentAsBytes)
                .map(KeyHelper::withHeader)
                .orElseThrow(() -> new NoSuchElementException("Missing identity"));
        var signedKey = node.findNode("skey")
                .flatMap(SignalSignedKeyPair::of)
                .orElseThrow(() -> new NoSuchElementException("Missing signed key"));
        var key = node.findNode("key")
                .flatMap(SignalSignedKeyPair::of)
                .orElse(null);
        var builder = new SessionBuilder(jid.toSignalAddress(), socket.keys());
        builder.createOutgoing(registrationId, identity, signedKey, key);
    }

    protected void decode(Node node) {
        var encrypted = node.findNodes("enc");
        encrypted.forEach(message -> decode(node, message));
    }

    private void decode(Node infoNode, Node messageNode) {
        try {
            var pushName = infoNode.attributes()
                    .getString("notify");
            var timestamp = infoNode.attributes()
                    .getLong("t");
            var id = infoNode.attributes()
                    .getRequiredString("id");
            var from = infoNode.attributes()
                    .getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var recipient = infoNode.attributes()
                    .getJid("recipient")
                    .orElse(from);
            var participant = infoNode.attributes()
                    .getJid("participant")
                    .orElse(null);
            var messageBuilder = MessageInfo.newMessageInfo();
            var keyBuilder = MessageKey.newMessageKeyBuilder();
            if(from.hasServer(ContactJid.Server.WHATSAPP) || from.hasServer(ContactJid.Server.USER)){
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                keyBuilder.fromMe(Objects.equals(from, socket.keys().companion().toUserJid()));
                messageBuilder.senderJid(from);
            }else {
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(requireNonNull(participant, "Missing participant in group message"));
                keyBuilder.fromMe(Objects.equals(participant.toUserJid(), socket.keys().companion().toUserJid()));
                messageBuilder.senderJid(requireNonNull(participant, "Missing participant in group message"));
            }

            var key = keyBuilder.id(id)
                    .build();
            var info = messageBuilder.key(key)
                    .pushName(pushName)
                    .timestamp(timestamp)
                    .build();

            socket.sendMessageAck(infoNode, of("class", "receipt"));
            var encodedMessage = messageNode.contentAsBytes()
                    .orElseThrow();
            var type = messageNode.attributes()
                    .getRequiredString("type");
            var decodedMessage = decodeMessageBytes(from, participant, encodedMessage, type);
            if(decodedMessage.isEmpty()){
                return;
            }

            var messageContainer = BytesHelper.bytesToMessage(decodedMessage.get());
            var message = messageContainer.content() instanceof DeviceSentMessage deviceSentMessage ?
                    MessageContainer.of(deviceSentMessage.message()
                            .content()) :
                    messageContainer;
            info.message(message);
            var content = info.message()
                    .content();
            if (content instanceof SenderKeyDistributionMessage distributionMessage) {
                handleDistributionMessage(distributionMessage, info.senderJid());
            }

            if (content instanceof ProtocolMessage protocolMessage) {
                handleProtocolMessage(info, protocolMessage, Objects.equals(infoNode.attributes()
                        .getString("category"), "peer"));
            }

            saveMessage(info);
            socket.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.key()
                    .id()));
        } catch (Throwable throwable) {
            socket.errorHandler()
                    .handleFailure(MESSAGE, throwable);
        }
    }

    private Optional<byte[]> decodeMessageBytes(ContactJid from, ContactJid participant, byte[] encodedMessage,
                                                String type) {
        try {
            lock.acquire();
            return Optional.ofNullable(switch (type) {
                case SKMSG -> {
                    Objects.requireNonNull(participant, "Cannot decipher skmsg without participant");
                    var senderName = new SenderKeyName(from.toString(), participant.toSignalAddress());
                    var signalGroup = new GroupCipher(senderName, socket.keys());
                    yield signalGroup.decrypt(encodedMessage);
                }

                case PKMSG -> {
                    var user = from.hasServer(ContactJid.Server.WHATSAPP) ?
                            from :
                            participant;
                    Objects.requireNonNull(user, "Cannot decipher pkmsg without user");

                    var session = new SessionCipher(user.toSignalAddress(), socket.keys());
                    var preKey = SignalPreKeyMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(preKey)
                            .orElse(null);
                }

                case MSG -> {
                    var user = from.hasServer(ContactJid.Server.WHATSAPP) ?
                            from :
                            participant;
                    Objects.requireNonNull(user, "Cannot decipher msg without user");

                    var session = new SessionCipher(user.toSignalAddress(), socket.keys());
                    var signalMessage = SignalMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(signalMessage);
                }

                default -> throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
            });
        } catch (Throwable throwable) {
            socket.errorHandler()
                    .handleFailure(MESSAGE, new RuntimeException(
                            "Cannot decrypt message with type %s inside %s from %s".formatted(type, from,
                                    requireNonNullElse(participant, from)), throwable));
            return Optional.empty();
        }finally {
            lock.release();
        }
    }

    private void saveMessage(MessageInfo info) {
        socket.store()
                .attribute(info);
        if (info.chatJid()
                .equals(ContactJid.STATUS_ACCOUNT)) {
            socket.store()
                    .addStatus(info);
            socket.onNewStatus(info);
            return;
        }

        info.chat()
                .addMessage(info);
        if (info.timestamp() <= socket.store()
                .initializationTimeStamp()) {
            return;
        }

        if (info.message()
                .hasCategory(MessageCategory.SERVER)) {
            return;
        }

        if (info.chat()
                .archived() && socket.store()
                .unarchiveChats()) {
            info.chat()
                    .archived(false);
        }

        info.chat()
                .unreadMessages(info.chat()
                        .unreadMessages() + 1);
        socket.onNewMessage(info);
    }

    private void handleDistributionMessage(SenderKeyDistributionMessage distributionMessage, ContactJid from) {
        var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
        var builder = new GroupBuilder(socket.keys());
        var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
        builder.createIncoming(groupName, message);
    }

    @SneakyThrows
    private void handleProtocolMessage(MessageInfo info, ProtocolMessage protocolMessage, boolean peer) {
        switch (protocolMessage.protocolType()) {
            case HISTORY_SYNC_NOTIFICATION -> {
                var compressed = Medias.download(protocolMessage.historySyncNotification())
                        .orElseThrow(() -> new IllegalArgumentException("Cannot download history sync"));
                var decompressed = BytesHelper.deflate(compressed);
                var history = PROTOBUF.readMessage(decompressed, HistorySync.class);
                switch (history.syncType()) {
                    case INITIAL_BOOTSTRAP -> {
                        history.conversations()
                                .forEach(this::addChatToHistory);
                        socket.store()
                                .hasSnapshot(true);
                        socket.store()
                                .invokeListeners(Listener::onChats);
                    }

                    case FULL -> history.conversations()
                            .forEach(this::addChatToHistory);

                    case INITIAL_STATUS_V3 -> {
                        history.statusV3Messages()
                                .forEach(socket.store()::addStatus);
                        socket.store()
                                .invokeListeners(Listener::onStatus);
                    }

                    case RECENT -> history.conversations()
                            .forEach(this::handleRecentMessage);

                    case PUSH_NAME -> {
                        history.pushNames()
                                .forEach(this::handNewPushName);
                        socket.store()
                                .invokeListeners(Listener::onContacts);
                    }

                    case null -> {}
                }

                socket.sendSyncReceipt(info, "hist_sync");
            }

            case APP_STATE_SYNC_KEY_SHARE -> {
                if (protocolMessage.appStateSyncKeyShare()
                        .keys()
                        .isEmpty()) {
                    return;
                }

                socket.keys()
                        .addAppKeys(protocolMessage.appStateSyncKeyShare()
                                .keys());
                socket.pullInitialPatches();
            }

            case REVOKE -> socket.store()
                    .findMessageById(info.chat(), protocolMessage.key()
                            .id())
                    .ifPresent(message -> {
                        info.chat()
                                .removeMessage(message);
                        socket.onMessageDeleted(message, true);
                    });

            case EPHEMERAL_SETTING -> {
                info.chat()
                        .ephemeralMessagesToggleTime(info.timestamp())
                        .ephemeralMessageDuration(ChatEphemeralTimer.forValue(protocolMessage.ephemeralExpiration()));
                var setting = new EphemeralSetting(info.ephemeralDuration(), info.timestamp());
                socket.onSetting(setting);
            }
        }

        // Save data to prevent session termination from messing up the cypher
        socket.store().serialize();
        if (!peer) {
            return;
        }

        socket.sendSyncReceipt(info, "peer_msg");
    }

    private void addChatToHistory(Chat chat) {
        socket.store()
                .addChat(chat);
        historyCache.put(chat, chat);
    }

    private void handNewPushName(PushName pushName) {
        var jid = ContactJid.of(pushName.id());
        socket.store()
                .findContactByJid(jid)
                .orElseGet(() -> socket.createContact(jid))
                .chosenName(pushName.name());
        var action = ContactAction.of(pushName.name(), null);
        socket.onAction(action);
    }

    private void handleRecentMessage(Chat recent) {
        socket.store()
                .findChatByJid(recent.jid())
                .ifPresentOrElse(knownChat -> {
                    // TODO: 30/06/2022 merge chats if needed
                    socket.onChatRecentMessages(knownChat, false);
                    historyCache.put(knownChat, knownChat);
                }, () -> {
                    socket.store()
                            .addChat(recent);
                    socket.onChatRecentMessages(recent, false);
                    historyCache.put(recent, recent);
                });
    }

    @SafeVarargs
    private <T> List<T> append(List<T>... all) {
        return Stream.of(all)
                .flatMap(Collection::stream)
                .toList();
    }
}
