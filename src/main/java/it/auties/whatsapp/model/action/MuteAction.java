package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.binary.BinaryPatchType;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.INT64;

/**
 * A model clas that represents a new mute status for a chat
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class MuteAction implements Action {
    /**
     * Whether this action marks the chat as muted
     */
    @ProtobufProperty(index = 1, type = BOOL)
    private boolean muted;

    /**
     * The timestamp of the of this mute
     */
    @ProtobufProperty(index = 2, type = INT64)
    private Long muteEndTimestampSeconds;

    /**
     * Auto mute
     */
    @ProtobufProperty(index = 3, name = "autoMuted", type = BOOL)
    private boolean autoMuted;

    /**
     * Returns when the mute ends
     *
     * @return an optional
     */
    public ZonedDateTime muteEnd() {
        return Clock.parseSeconds(muteEndTimestampSeconds);
    }

    /**
     * Returns when the mute ends in seconds
     *
     * @return a long
     */
    public long muteEndTimestampSeconds() {
        return muteEndTimestampSeconds == null ? 0 : muteEndTimestampSeconds;
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "mute";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 2;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public BinaryPatchType actionType() {
        return null;
    }
}