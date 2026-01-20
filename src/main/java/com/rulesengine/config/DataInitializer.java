package com.rulesengine.config;

import com.rulesengine.entity.UserEntity;
import com.rulesengine.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUser();
    }

    private void initializeDefaultUser() {
        if (!userRepository.existsByUsername("admin")) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@rulesengine.com");
            admin.setFullName("System Administrator");
            admin.setRole(UserEntity.Role.ADMIN);
            admin.setIsActive(true);

            userRepository.save(admin);
            logger.info("Default admin user created with username: admin, password: admin123");
        }

        if (!userRepository.existsByUsername("user")) {
            UserEntity user = new UserEntity();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@rulesengine.com");
            user.setFullName("Test User");
            user.setRole(UserEntity.Role.USER);
            user.setIsActive(true);

            userRepository.save(user);
            logger.info("Default test user created with username: user, password: user123");
        }
    }
}