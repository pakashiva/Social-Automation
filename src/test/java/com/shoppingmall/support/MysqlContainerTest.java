package com.shoppingmall.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MysqlContainerTest extends MallSpringTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void versionStartsWith8413() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT VERSION()")) {
            assertTrue(result.next());
            assertTrue(result.getString(1).startsWith("8.4.13"), result.getString(1));
        }
    }
}
