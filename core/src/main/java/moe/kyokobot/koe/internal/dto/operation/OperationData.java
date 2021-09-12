package moe.kyokobot.koe.internal.dto.operation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import moe.kyokobot.koe.internal.dto.Data;
import moe.kyokobot.koe.internal.dto.Operation;

public class OperationData extends Operation {
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "op"
    )
    @JsonProperty("d")
    public final Data data;

    public OperationData(int opCode, Data data) {
        this.opCode = opCode;
        this.data = data;
    }
}
