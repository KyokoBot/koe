package moe.kyokobot.koe.testbot.util;

import moe.kyokobot.koe.testbot.TestBot;

import java.util.ArrayList;

public class GCPressureGenerator {
    @SuppressWarnings("squid:S1215")
    private static final Thread pressureThread = new Thread(() -> {
        try {
            while (true) {
                try {
                    {
                        ArrayList<int[]> l = new ArrayList<>();
                        for (int i = 0; i < 10000; i++) {
                            l.add(new int[1024]);
                        }
                        l.stream().map(String::valueOf).count();
                    }
                    {
                        for (int i = 0; i < 25000; i++) {
                            String[] arr = "malksmdlkamsldmalksmdlkmasldmlkam32908092930180928308290488209830928081028013sldmlkamslkdmlakmsldkmlakmsldkmalsmdalksmldaads".split(String.valueOf(i));
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
