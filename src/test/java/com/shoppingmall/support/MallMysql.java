package com.shoppingmall.support;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.MySQLContainer;

public final class MallMysql {

    @SuppressWarnings("resource")
    public static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.13")
            .withDatabaseName("shopping_mall")
            .withUsername("mall")
            .withPassword("mall");

    private static final String UPLOAD_DIR;

    static {
        MYSQL.start();
        UPLOAD_DIR = createUploadDir();
        SchemaLoader.load(jdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        System.setProperty("SM_DB_URL", jdbcUrl());
        System.setProperty("SM_DB_USER", MYSQL.getUsername());
        System.setProperty("SM_DB_PASSWORD", MYSQL.getPassword());
        System.setProperty("SM_UPLOAD_DIR", UPLOAD_DIR);
    }

    private MallMysql() {
    }

    public static String jdbcUrl() {
        String url = MYSQL.getJdbcUrl();
        String extra = "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
                + "&allowPublicKeyRetrieval=true&characterEncoding=utf8&useSSL=false";
        return url.contains("?") ? url + "&" + extra : url + "?" + extra;
    }

    public static String uploadDir() {
        return UPLOAD_DIR;
    }

    private static String createUploadDir() {
        try {
            Path dir = Files.createTempDirectory("mall-uploads");
            return dir.toAbsolutePath().toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create the upload directory.", exception);
        }
    }
}
