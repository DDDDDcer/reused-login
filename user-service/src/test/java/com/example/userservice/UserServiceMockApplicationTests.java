package com.example.userservice;

import com.example.userservice.shiro.UserRealm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceMockApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文启动正常
    }

    @Test
    void testSha256() {
        String salt = UserRealm.generateSalt();
        assertNotNull(salt, "盐值不应为空");
        assertTrue(salt.length() > 0, "盐值长度应大于0");

        String hash1 = UserRealm.sha256(salt + "123456");
        String hash2 = UserRealm.sha256(salt + "123456");
        assertEquals(hash1, hash2, "相同输入应产生相同哈希");

        String hash3 = UserRealm.sha256(salt + "654321");
        assertNotEquals(hash1, hash3, "不同输入应产生不同哈希");
    }

    @Test
    void testGenerateSaltUnique() {
        String salt1 = UserRealm.generateSalt();
        String salt2 = UserRealm.generateSalt();
        assertNotEquals(salt1, salt2, "每次生成的盐值应不同");
    }

    @Test
    void testSha256Length() {
        String hash = UserRealm.sha256("test");
        assertEquals(64, hash.length(), "SHA-256 哈希应为 64 个十六进制字符");
    }
}