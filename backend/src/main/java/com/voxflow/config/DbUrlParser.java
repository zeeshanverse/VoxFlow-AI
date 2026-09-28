package com.voxflow.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts either a JDBC URL (jdbc:postgresql://host/db?sslmode=require) or a
 * provider-style URI (postgresql://user:pass@host/db?sslmode=require, as shown
 * by Neon / Render / Heroku) and turns it into a JDBC URL plus credentials.
 * Pure JDK, no Spring dependency, so it is easy to test.
 */
final class DbUrlParser {

    record Result(String jdbcUrl, String username, String password) {}

    private DbUrlParser() {}

    static Result parse(String rawUrl, String fallbackUser, String fallbackPassword) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("DB_URL is empty");
        }

        String url = rawUrl.trim();
        String user = fallbackUser;
        String pass = fallbackPassword;

        if (url.startsWith("jdbc:")) {
            return new Result(cleanQuery(url), user, pass);
        }

        String rest;
        if (url.startsWith("postgresql://")) {
            rest = url.substring("postgresql://".length());
        } else if (url.startsWith("postgres://")) {
            rest = url.substring("postgres://".length());
        } else {
            throw new IllegalArgumentException(
                "DB_URL must start with jdbc:postgresql:// or postgresql://");
        }

        // Split "userinfo@hostpart" on the LAST '@' (passwords may contain '@' if not encoded).
        int at = rest.lastIndexOf('@');
        String hostPart = rest;
        if (at >= 0) {
            String userInfo = rest.substring(0, at);
            hostPart = rest.substring(at + 1);
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                user = decode(userInfo.substring(0, colon));
                pass = decode(userInfo.substring(colon + 1));
            } else if (!userInfo.isEmpty()) {
                user = decode(userInfo);
            }
        }

        String jdbc = "jdbc:postgresql://" + hostPart;
        return new Result(cleanQuery(jdbc), user, pass);
    }

    /** Removes params the Java driver rejects and adds sslmode=require for remote hosts. */
    private static String cleanQuery(String jdbcUrl) {
        int q = jdbcUrl.indexOf('?');
        String base = q >= 0 ? jdbcUrl.substring(0, q) : jdbcUrl;
        String query = q >= 0 ? jdbcUrl.substring(q + 1) : "";

        List<String> kept = new ArrayList<>();
        boolean hasSsl = false;
        for (String p : query.split("&")) {
            if (p.isBlank()) continue;
            String key = p.contains("=") ? p.substring(0, p.indexOf('=')) : p;
            if (key.equalsIgnoreCase("channel_binding")) continue;
            if (key.equalsIgnoreCase("sslmode")) hasSsl = true;
            kept.add(p);
        }

        boolean local = base.contains("//localhost") || base.contains("//127.0.0.1");
        if (!hasSsl && !local) kept.add("sslmode=require");

        return kept.isEmpty() ? base : base + "?" + String.join("&", kept);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}