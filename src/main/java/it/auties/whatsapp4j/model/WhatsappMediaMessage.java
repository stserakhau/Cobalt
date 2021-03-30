package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds media inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
@ToString
public final class WhatsappMediaMessage extends WhatsappUserMessage {
    /**
     * The type of media that this object wraps
     */
    private final @NotNull WhatsappMediaMessageType type;

    /**
     * Constructs a WhatsappMediaMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappMediaMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasImageMessage() || info.getMessage().hasDocumentMessage() || info.getMessage().hasVideoMessage() || info.getMessage().hasStickerMessage());
        this.type = WhatsappMediaMessageType.fromMessage(info.getMessage());
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        var message = info.getMessage();
        if(message.hasImageMessage()){
            return message.getImageMessage().hasContextInfo() ? Optional.of(message.getImageMessage().getContextInfo()) : Optional.empty();
        }

        if(message.hasDocumentMessage()){
            return message.getDocumentMessage().hasContextInfo() ? Optional.of(message.getDocumentMessage().getContextInfo()) : Optional.empty();
        }

        if(message.hasVideoMessage()){
            return message.getVideoMessage().hasContextInfo() ? Optional.of(message.getVideoMessage().getContextInfo()) : Optional.empty();
        }

        return message.getStickerMessage().hasContextInfo() ? Optional.of(message.getStickerMessage().getContextInfo()) : Optional.empty();
    }

    //TODO: Decrypt file and provide an accessor to access it
}
