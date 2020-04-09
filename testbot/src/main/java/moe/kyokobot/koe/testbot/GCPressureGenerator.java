package moe.kyokobot.koe.testbot;

import moe.kyokobot.koe.KoeOptions;

import java.util.ArrayList;

public class GCPressureGenerator {
    @SuppressWarnings("squid:S1215")
    private static Thread pressureThread = new Thread(() -> {
        try {
            while (true) {
                try {
                    {
                        var l = new ArrayList<int[]>();
                        for (int i = 0; i < 10000; i++) {
                            l.add(new int[1024]);
                        }
                        l.stream().map(String::valueOf).count();
                    }
                    {
                        for (int i = 0; i < 25000; i++) {
                            var arr = "malksmdlkamsldmalksmdlkmasldmlkam32908092930180928308290488209830928081028013sldmlkamslkdmlakmsldkmlakmsldkmalsmdalksmldaads".split(String.valueOf(i));
                        }
                    }
                    {
                        for (int i = 0; i < 25000; i++) {
                            new TestBot(null);
                        }
                    }

                    long pre = System.currentTimeMillis();
                    System.gc();
                    System.out.printf("GC took %dms\n", System.currentTimeMillis() - pre);
                } catch (OutOfMemoryError e) {
                    long pre = System.currentTimeMillis();
                    System.gc();
                    System.out.printf("OOM! GC took %dms\n", System.currentTimeMillis() - pre);
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }, "Koe TestBot - GC Pressure Tester");

    public static boolean toggle() {
        if (pressureThread.isAlive()) {
            pressureThread.interrupt();
            return false;
        } else {
            pressureThread.start();
            return true;
        }
    }
}
