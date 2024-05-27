package com.example;

import java.util.Random;

public class Hello {
    public static void m1() {
        System.out.println("m1");

        int r = new Random().nextInt(5);
        if (r > 3) {
            m2();
        } else {
            m3();
        }
    }

    public static void m2() {
        System.out.println("m2");
        m4();
    }

    public static void m3() {
        System.out.println("m3");
        m5();
    }

    public static void m4() {
        System.out.println("m4");
    }

    public static void m5() {
        System.out.println("m5");
    }
}
