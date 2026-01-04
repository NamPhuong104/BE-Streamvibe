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

import java.util.HashSet;
import java.util.Set;

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
    }

    private void initRole() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (!roleRepository.existsByName(roleEnum.getName())) {
                Role role = Role.builder()
                        .name(roleEnum.getName())
                        .description(roleEnum.getDescription())
                        .priority(roleEnum.getPriority())
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
            Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER.getName()).orElseThrow(() -> new RuntimeException("USER role not found"));

            User superAdmin = User.builder()
                    .email(superAdminEmail)
                    .username("superadmin")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Super Administrator")
                    .provider("LOCAL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .roles(new HashSet<>(Set.of(superAdminRole, userRole)))
                    .build();

            userRepository.save(superAdmin);
            log.info("✅ Created Super Admin: {} / SuperAdmin@123", superAdminEmail);
        }
    }
}
