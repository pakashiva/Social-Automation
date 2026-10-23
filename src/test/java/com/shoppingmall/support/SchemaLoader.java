package com.shoppingmall.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SchemaLoader {

    private SchemaLoader() {
    }

    public static void load(String jdbcUrl, String user, String password) {
        try {
            String sql = Files.readString(Path.of("sql/schema.sql"));
            try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
                    Statement statement = connection.createStatement()) {
                statement.execute("SET NAMES utf8mb4");
                statement.execute(
                        "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,ERROR_FOR_DIVISION_BY_ZERO'");
                for (String command : statements(sql)) {
                    String lead = command.stripLeading().toUpperCase();
                    if (lead.startsWith("CREATE DATABASE") || lead.startsWith("USE ")) {
                        continue;
                    }
                    statement.execute(command);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load sql/schema.sql.", exception);
        }
    }

    static List<String> statements(String sql) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\n", -1)) {
            if (line.stripLeading().startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (line.stripTrailing().endsWith(";")) {
                String command = current.toString().trim();
                if (command.endsWith(";")) {
                    command = command.substring(0, command.length() - 1).trim();
                }
                if (!command.isEmpty()) {
                    commands.add(command);
                }
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            commands.add(tail);
        }
        return commands;
    }
}
