package movieapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.entity.Role;
import movieapp.entity.User;
import movieapp.repository.RoleRepository;
import movieapp.repository.UserRepository;
import movieapp.util.constant.RoleEnum;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)// Chạy trước các initializer khác
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRole();
        initSuperAdmin();
        initAdmin();
        initUser();
    }

    private void initRole() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (!roleRepository.existsByName(roleEnum.getName())) {
                Role role = Role.builder()
                        .name(roleEnum.getName())
                        .description(roleEnum.getDescription())
                        .priority(roleEnum.getPriority())
                        .isSystemRole(roleEnum.isSystemRole())
                        .build();
                roleRepository.save(role);
                log.info("✅ Created role: {}", roleEnum.getName());
            }
        }
    }

    private void initSuperAdmin() {
        String superAdminEmail = "superadmin@streamvibe.com";

        if (!userRepository.existsByEmail(superAdminEmail)) {
            Role superAdminRole = roleRepository.findByName(RoleEnum.ROLE_SUPER_ADMIN.getName()).orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

            User superAdmin = User.builder()
                    .email(superAdminEmail)
                    .username("superadmin")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Super Administrator")
                    .provider("LOCAL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .role(superAdminRole)
                    .build();

            userRepository.save(superAdmin);
            log.info("✅ Created Super Admin: {} / SuperAdmin@123", superAdminEmail);
        }
    }

    private void initAdmin() {
        String adminEmail = "admin@streamvibe.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            Role superAdminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN.getName()).orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            User superAdmin = User.builder()
                    .email(adminEmail)
                    .username("admin")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Administrator")
                    .provider("LOCAL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .role(superAdminRole)
                    .build();

            userRepository.save(superAdmin);
            log.info("✅ Created Admin: {} / Admin", adminEmail);
        }
    }

    private void initUser() {
        String userEmail = "user@streamvibe.com";

        if (!userRepository.existsByEmail(userEmail)) {
            Role superAdminRole = roleRepository.findByName(RoleEnum.ROLE_USER.getName()).orElseThrow(() -> new RuntimeException("USER role not found"));

            User superAdmin = User.builder()
                    .email(userEmail)
                    .username("user")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("User")
                    .provider("LOCAL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .role(superAdminRole)
                    .build();

            userRepository.save(superAdmin);
            log.info("✅ Created User: {} / User", userEmail);
        }
    }
}
