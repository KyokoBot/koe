module moe.kyokobot.koe {
    requires io.netty.all;
    requires org.slf4j;
    requires annotations;
    
    exports moe.kyokobot.koe;
    exports moe.kyokobot.koe.crypto;
    exports moe.kyokobot.koe.gateway;
    exports moe.kyokobot.koe.audio;
}