package moe.kyokobot.koe.dave.mls;

import java.util.HashMap;

public interface RosterVariant {
    class Failed implements RosterVariant {
        public Failed() {

        }
    }

    class Ignored implements RosterVariant {
        public Ignored() {

        }
    }

    class RosterMap implements RosterVariant {
        private final HashMap<Long, byte[]> inner;

        public RosterMap(HashMap<Long, byte[]> inner) {
            this.inner = inner;
        }

        public HashMap<Long, byte[]> getInner() {
            return inner;
        }

        public byte[] get(long key) {
            return inner.get(key);
        }
    }
}
