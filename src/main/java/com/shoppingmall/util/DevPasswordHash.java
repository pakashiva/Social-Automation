package com.shoppingmall.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class DevPasswordHash {

    private DevPasswordHash() {
    }

    public static void main(String[] args) {
        String password = System.getenv("SM_DEV_SEED_PASSWORD");
        if (password == null || password.length() < 8 || password.length() > 72) {
            System.err.println("Password must be 8 to 72 characters.");
            System.exit(1);
            return;
        }
        System.out.println(new BCryptPasswordEncoder(10).encode(password));
    }
}
