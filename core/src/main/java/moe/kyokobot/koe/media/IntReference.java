package moe.kyokobot.koe.media;

/**
 * Mutable reference to an int value. Provides no atomicity guarantees
 * and should not be shared between threads without external synchronization.
 */
public class IntReference {
    private int value;
    
    public int get() {
        return value;
    }
    
    public void set(int value) {
        this.value = value;
    }
    
    public void add(int amount) {
        this.value += amount;
    }
}
