package moe.kyokobot.koe.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Operation {
    @JsonProperty("op")
    public int opCode;
}
