package com.shoppingmall.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class MallSpringTest {

    @Autowired
    protected TestData data;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("SM_DB_URL", MallMysql::jdbcUrl);
        registry.add("SM_DB_USER", MallMysql.MYSQL::getUsername);
        registry.add("SM_DB_PASSWORD", MallMysql.MYSQL::getPassword);
        registry.add("SM_UPLOAD_DIR", MallMysql::uploadDir);
    }

    @BeforeEach
    void roles() {
        data.ensureRoles();
    }
}
