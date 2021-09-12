package moe.kyokobot.koe.internal.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import moe.kyokobot.koe.gateway.Op;
import moe.kyokobot.koe.internal.dto.data.*;

@JsonSubTypes({
        @JsonSubTypes.Type(value = Hello.class, name = "" + Op.HELLO),
        @JsonSubTypes.Type(value = Ready.class, name = "" + Op.READY),
        @JsonSubTypes.Type(value = SessionDescription.class, name = "" + Op.SESSION_DESCRIPTION),
        @JsonSubTypes.Type(value = ClientConnect.class, name = "" + Op.CLIENT_CONNECT),
        @JsonSubTypes.Type(value = ClientDisconnect.class, name = "" + Op.CLIENT_DISCONNECT),
        @JsonSubTypes.Type(value = Identify.class, name = "" + Op.IDENTIFY),
        @JsonSubTypes.Type(value = SelectProtocol.class, name = "" + Op.SELECT_PROTOCOL),
        @JsonSubTypes.Type(value = Speaking.class, name = "" + Op.SPEAKING),
        @JsonSubTypes.Type(value = VideoSinkWants.class, name = "" + Op.VIDEO_SINK_WANTS)
})
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
public interface Data {
}
