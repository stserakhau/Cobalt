package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.CallInfo;
import it.auties.whatsapp.model.info.MessageContextInfo;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.device.DeviceSentMessage;
import it.auties.whatsapp.model.message.device.DeviceSyncMessage;
import it.auties.whatsapp.model.message.payment.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.message.server.StickerSyncRMRMessage;
import it.auties.whatsapp.model.message.standard.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A container for all types of messages known currently to WhatsappWeb.
 * <p>
 * Only one of these properties should be populated, however it's not certain as Whatsapp's Protobuf doesn't use a oneof instruction as it would be logical to in said case.
 * This may imply that in some particular and rare cases more than one property can be populated.
 * <p>
 * There are several categories of messages:
 * <ul>
 *     <li>Server messages</li>
 *     <li>Button messages</li>
 *     <li>Product messages</li>
 *     <li>Payment messages</li>
 *     <li>Standard messages</li>
 * </ul>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "newMessageContainerBuilder")
@Jacksonized
@Accessors(fluent = true)
public class MessageContainer implements ProtobufMessage {
    private static final EmptyMessage EMPTY_MESSAGE = new EmptyMessage();

    @ProtobufProperty(index = 1, type = STRING)
    private String textWithNoContext;

    /**
     * Sender key distribution message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = SenderKeyDistributionMessage.class)
    private SenderKeyDistributionMessage senderKeyDistribution;

    /**
     * Image message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ImageMessage.class)
    private ImageMessage image;

    /**
     * Contact message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ContactMessage.class)
    private ContactMessage contact;

    /**
     * Location message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = LocationMessage.class)
    private LocationMessage location;

    /**
     * Text message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = TextMessage.class)
    private TextMessage text;

    /**
     * Document message
     */
    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = DocumentMessage.class)
    private DocumentMessage document;

    /**
     * Audio message
     */
    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = AudioMessage.class)
    private AudioMessage audio;

    /**
     * Video message
     */
    @ProtobufProperty(index = 9, type = MESSAGE, concreteType = VideoMessage.class)
    private VideoMessage video;

    /**
     * Call message
     */
    @ProtobufProperty(index = 10, type = MESSAGE, concreteType = CallInfo.class)
    private CallInfo call;

    /**
     * Sever message
     */
    @ProtobufProperty(index = 12, type = MESSAGE, concreteType = ProtocolMessage.class)
    private ProtocolMessage protocol;

    /**
     * Contact array message
     */
    @ProtobufProperty(index = 13, type = MESSAGE, concreteType = ContactsArrayMessage.class)
    private ContactsArrayMessage contactsArray;

    /**
     * Highly structured message
     */
    @ProtobufProperty(index = 14, type = MESSAGE, concreteType = HighlyStructuredMessage.class)
    private HighlyStructuredMessage highlyStructured;

    /**
     * Send payment message
     */
    @ProtobufProperty(index = 16, type = MESSAGE, concreteType = SendPaymentMessage.class)
    private SendPaymentMessage sendPayment;

    /**
     * Live location message
     */
    @ProtobufProperty(index = 18, type = MESSAGE, concreteType = LiveLocationMessage.class)
    private LiveLocationMessage liveLocation;

    /**
     * Request payment message
     */
    @ProtobufProperty(index = 22, type = MESSAGE, concreteType = RequestPaymentMessage.class)
    private RequestPaymentMessage requestPayment;

    /**
     * Decline payment request message
     */
    @ProtobufProperty(index = 23, type = MESSAGE, concreteType = DeclinePaymentRequestMessage.class)
    private DeclinePaymentRequestMessage declinePaymentRequest;

    /**
     * Cancel payment request message
     */
    @ProtobufProperty(index = 24, type = MESSAGE, concreteType = CancelPaymentRequestMessage.class)
    private CancelPaymentRequestMessage cancelPaymentRequest;

    /**
     * Template message
     */
    @ProtobufProperty(index = 25, type = MESSAGE, concreteType = TemplateMessage.class)
    private TemplateMessage template;

    /**
     * Sticker message
     */
    @ProtobufProperty(index = 26, type = MESSAGE, concreteType = StickerMessage.class)
    private StickerMessage sticker;

    /**
     * Group invite message
     */
    @ProtobufProperty(index = 28, type = MESSAGE, concreteType = GroupInviteMessage.class)
    private GroupInviteMessage groupInvite;

    /**
     * Template button reply message
     */
    @ProtobufProperty(index = 29, type = MESSAGE, concreteType = TemplateReplyMessage.class)
    private TemplateReplyMessage templateReply;

    /**
     * Product message
     */
    @ProtobufProperty(index = 30, type = MESSAGE, concreteType = ProductMessage.class)
    private ProductMessage product;

    /**
     * Device sent message
     */
    @ProtobufProperty(index = 31, type = MESSAGE, concreteType = DeviceSentMessage.class)
    private DeviceSentMessage deviceSent;

    /**
     * Device dataSync message
     */
    @ProtobufProperty(index = 32, type = MESSAGE, concreteType = DeviceSyncMessage.class)
    private DeviceSyncMessage deviceSync;

    /**
     * List message
     */
    @ProtobufProperty(index = 36, type = MESSAGE, concreteType = ListMessage.class)
    private ListMessage list;

    /**
     * View once message
     */
    @ProtobufProperty(index = 37, type = MESSAGE, concreteType = FutureMessageContainer.class)
    private FutureMessageContainer viewOnce;

    /**
     * Order message
     */
    @ProtobufProperty(index = 38, type = MESSAGE, concreteType = PaymentOrderMessage.class)
    private PaymentOrderMessage order;

    /**
     * List response message
     */
    @ProtobufProperty(index = 39, type = MESSAGE, concreteType = ListResponseMessage.class)
    private ListResponseMessage listResponse;

    /**
     * Ephemeral message
     */
    @ProtobufProperty(index = 40, type = MESSAGE, concreteType = FutureMessageContainer.class)
    private FutureMessageContainer ephemeral;

    /**
     * Invoice message
     */
    @ProtobufProperty(index = 41, type = MESSAGE, concreteType = PaymentInvoiceMessage.class)
    private PaymentInvoiceMessage invoice;

    /**
     * Buttons message
     */
    @ProtobufProperty(index = 42, type = MESSAGE, concreteType = ButtonsMessage.class)
    private ButtonsMessage buttons;

    /**
     * Buttons response message
     */
    @ProtobufProperty(index = 43, type = MESSAGE, concreteType = ButtonsResponseMessage.class)
    private ButtonsResponseMessage buttonsResponse;

    /**
     * Payment invite message
     */
    @ProtobufProperty(index = 44, type = MESSAGE, concreteType = PaymentInviteMessage.class)
    private PaymentInviteMessage paymentInvite;

    /**
     * Interactive message
     */
    @ProtobufProperty(index = 45, type = MESSAGE, concreteType = InteractiveMessage.class)
    private InteractiveMessage interactive;

    /**
     * Reaction message
     */
    @ProtobufProperty(index = 46, type = MESSAGE, concreteType = ReactionMessage.class)
    private ReactionMessage reaction;

    /**
     * Sticker sync message
     */
    @ProtobufProperty(index = 47, type = MESSAGE, concreteType = StickerSyncRMRMessage.class)
    private StickerSyncRMRMessage stickerSync;

    /**
     * Message context info
     */
    @ProtobufProperty(index = 35, type = MESSAGE, concreteType = MessageContextInfo.class)
    @Setter
    private MessageContextInfo deviceInfo;

    /**
     * Constructs a new MessageContainerBuilder from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     * @return a non-null builder
     */
    public static <T extends Message> MessageContainerBuilder newMessageContainerBuilder(@NonNull T message) {
        if(message instanceof DeviceSentMessage deviceSent) {
            return newMessageContainerBuilder(deviceSent.message().content());
        }

        var builder = MessageContainer.newMessageContainerBuilder();
        switch (message) {
            case EmptyMessage ignored -> {}
            case SenderKeyDistributionMessage senderKeyDistribution ->
                    builder.senderKeyDistribution(senderKeyDistribution);
            case ImageMessage image -> builder.image(image);
            case ContactMessage contact -> builder.contact(contact);
            case LocationMessage location -> builder.location(location);
            case TextMessage text -> builder.text(text);
            case DocumentMessage document -> builder.document(document);
            case AudioMessage audio -> builder.audio(audio);
            case VideoMessage video -> builder.video(video);
            case ProtocolMessage protocol -> builder.protocol(protocol);
            case ContactsArrayMessage contactsArray -> builder.contactsArray(contactsArray);
            case HighlyStructuredMessage highlyStructured -> builder.highlyStructured(highlyStructured);
            case SendPaymentMessage sendPayment -> builder.sendPayment(sendPayment);
            case LiveLocationMessage liveLocation -> builder.liveLocation(liveLocation);
            case RequestPaymentMessage requestPayment -> builder.requestPayment(requestPayment);
            case DeclinePaymentRequestMessage declinePaymentRequest ->
                    builder.declinePaymentRequest(declinePaymentRequest);
            case CancelPaymentRequestMessage cancelPaymentRequest -> builder.cancelPaymentRequest(cancelPaymentRequest);
            case TemplateMessage template -> builder.template(template);
            case StickerMessage sticker -> builder.sticker(sticker);
            case GroupInviteMessage groupInvite -> builder.groupInvite(groupInvite);
            case TemplateReplyMessage templateButtonReply -> builder.templateReply(templateButtonReply);
            case ProductMessage product -> builder.product(product);
            case DeviceSyncMessage deviceSync -> builder.deviceSync(deviceSync);
            case ListMessage buttonsList -> builder.list(buttonsList);
            case PaymentOrderMessage order -> builder.order(order);
            case ListResponseMessage listResponse -> builder.listResponse(listResponse);
            case PaymentInvoiceMessage invoice -> builder.invoice(invoice);
            case ButtonsMessage buttons -> builder.buttons(buttons);
            case ButtonsResponseMessage buttonsResponse -> builder.buttonsResponse(buttonsResponse);
            case PaymentInviteMessage paymentInvite -> builder.paymentInvite(paymentInvite);
            case InteractiveMessage interactive -> builder.interactive(interactive);
            case ReactionMessage reaction -> builder.reaction(reaction);
            case StickerSyncRMRMessage stickerSync -> builder.stickerSync(stickerSync);
            default -> throw new IllegalStateException("Unsupported message: " + message);
        }

        return builder;
    }

    /**
     * Constructs a new MessageContainerBuilder
     *
     * @return a non-null builder
     */
    public static MessageContainerBuilder newMessageContainerBuilder(){
        return new MessageContainerBuilder();
    }

    /**
     * Constructs a new MessageContainer from a text message
     *
     * @param message the text message with no context
     */
    public static MessageContainer of(@NonNull String message) {
        return MessageContainer.newMessageContainerBuilder()
                .text(TextMessage.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     * @return a non-null container
     */
    public static <T extends Message> MessageContainer of(@NonNull T message) {
        return newMessageContainerBuilder(message)
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type that can only be seen once
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofViewOnce(@NonNull T message) {
        return MessageContainer.newMessageContainerBuilder()
                .viewOnce(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type marking it as ephemeral
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEphemeral(@NonNull T message) {
        return MessageContainer.newMessageContainerBuilder()
                .ephemeral(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a device message.
     * Unlike {@link MessageContainer#of(Message)} and {@link MessageContainer#newMessageContainerBuilder(Message)} it doesn't unwrap it.
     *
     * @param message the text message with no context
     */
    public static MessageContainer ofDeviceMessage(@NonNull DeviceSentMessage message) {
        return MessageContainer.newMessageContainerBuilder()
                .deviceSent(message)
                .build();
    }

    /**
     * Returns the first populated message inside this container.
     * If no message is found, {@link EmptyMessage} is returned
     *
     * @return a non-null message
     */
    public Message content() {
        if (this.textWithNoContext != null)
            return TextMessage.of(textWithNoContext);
        if (this.senderKeyDistribution != null)
            return senderKeyDistribution;
        if (this.image != null)
            return image;
        if (this.contact != null)
            return contact;
        if (this.location != null)
            return location;
        if (this.text != null)
            return text;
        if (this.document != null)
            return document;
        if (this.audio != null)
            return audio;
        if (this.video != null)
            return video;
        if (this.protocol != null)
            return protocol;
        if (this.contactsArray != null)
            return contactsArray;
        if (this.highlyStructured != null)
            return highlyStructured;
        if (this.sendPayment != null)
            return sendPayment;
        if (this.liveLocation != null)
            return liveLocation;
        if (this.requestPayment != null)
            return requestPayment;
        if (this.declinePaymentRequest != null)
            return declinePaymentRequest;
        if (this.cancelPaymentRequest != null)
            return cancelPaymentRequest;
        if (this.template != null)
            return template;
        if (this.sticker != null)
            return sticker;
        if (this.groupInvite != null)
            return groupInvite;
        if (this.templateReply != null)
            return templateReply;
        if (this.product != null)
            return product;
        if (this.deviceSent != null)
            return deviceSent;
        if (this.deviceSync != null)
            return deviceSync;
        if (this.list != null)
            return list;
        if (this.viewOnce != null)
            return viewOnce.unbox();
        if (this.order != null)
            return order;
        if (this.listResponse != null)
            return listResponse;
        if (this.ephemeral != null)
            return ephemeral.unbox();
        if (this.invoice != null)
            return invoice;
        if (this.buttons != null)
            return buttons;
        if (this.buttonsResponse != null)
            return buttonsResponse;
        if (this.paymentInvite != null)
            return paymentInvite;
        if (interactive != null)
            return interactive;
        if (reaction != null)
            return reaction;
        if (stickerSync != null)
            return stickerSync;
        return EMPTY_MESSAGE;
    }

    /**
     * Returns the first populated contextual message inside this container
     *
     * @return a non-null Optional ContextualMessage
     */
    public Optional<ContextualMessage> contentWithContext() {
        if (this.image != null)
            return Optional.of(image);
        if (this.contact != null)
            return Optional.of(contact);
        if (this.location != null)
            return Optional.of(location);
        if (this.text != null)
            return Optional.of(text);
        if (this.document != null)
            return Optional.of(document);
        if (this.audio != null)
            return Optional.of(audio);
        if (this.video != null)
            return Optional.of(video);
        if (this.contactsArray != null)
            return Optional.of(contactsArray);
        if (this.liveLocation != null)
            return Optional.of(liveLocation);
        if (this.template != null)
            return Optional.of(template);
        if (this.sticker != null)
            return Optional.of(sticker);
        if (this.groupInvite != null)
            return Optional.of(groupInvite);
        if (this.templateReply != null)
            return Optional.of(templateReply);
        if (this.product != null)
            return Optional.of(product);
        if (this.list != null)
            return Optional.of(list);
        if (this.invoice != null)
            return Optional.of(invoice);
        if (this.buttons != null)
            return Optional.of(buttons);
        if (this.buttonsResponse != null)
            return Optional.of(buttonsResponse);
        if (this.viewOnce != null && viewOnce.unbox() instanceof ContextualMessage contextualViewOnce)
            return Optional.of(contextualViewOnce);
        if (this.ephemeral != null && ephemeral.unbox() instanceof ContextualMessage contextualEphemeral)
            return Optional.of(contextualEphemeral);
        return Optional.empty();
    }

    /**
     * Checks whether the message that this container wraps matches the provided type
     *
     * @param type the non-null type to check against
     * @return a boolean
     */
    public boolean hasType(@NonNull MessageType type){
        return content().type() == type;
    }

    /**
     * Checks whether the message that this container wraps matches the provided category
     *
     * @param category the non-null category to check against
     * @return a boolean
     */
    public boolean hasCategory(@NonNull MessageCategory category){
        return content().category() == category;
    }

    /**
     * Returns the type of the message
     *
     * @return a non-null type
     */
    public MessageType type() {
        return ephemeral != null ? MessageType.EPHEMERAL : viewOnce != null ? MessageType.VIEW_ONCE : content().type();
    }

    /**
     * Returns the deep type of the message unwrapping ephemeral and view once messages
     *
     * @return a non-null type
     */
    public MessageType deepType() {
        return content().type();
    }

    /**
     * Returns the category of the message
     *
     * @return a non-null category
     */
    public MessageCategory category(){
        return content().category();
    }

    /**
     * Returns the call wrapped by this message, if any is present
     *
     * @return a non-null optional
     */
    public Optional<CallInfo> call() {
        return Optional.ofNullable(call);
    }

    /**
     * Returns the device info, if any is present
     *
     * @return a non-null optional
     */
    public Optional<MessageContextInfo> deviceInfo() {
        return Optional.ofNullable(deviceInfo);
    }

    /**
     * Converts this message to an ephemeral message
     *
     * @return a non-null message container
     */
    public MessageContainer toEphemeral() {
        return newMessageContainerBuilder()
                .ephemeral(FutureMessageContainer.of(content()))
                .call(call)
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Converts this message to a view once message
     *
     * @return a non-null message container
     */
    public MessageContainer toViewOnce() {
        return newMessageContainerBuilder()
                .viewOnce(FutureMessageContainer.of(content()))
                .call(call)
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Returns whether this container is empty
     *
     * @return a boolean
     */
    public boolean isEmpty() {
        return hasCategory(MessageCategory.EMPTY);
    }

    /**
     * Converts this container into a String
     *
     * @return a non-null String
     */
    @Override
    public String toString() {
        return Objects.toString(content());
    }
}
