package movieapp.util.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum RoleEnum {
    ROLE_SUPER_ADMIN("ROLE_SUPER_ADMIN", "Super Administrator - Full access", 0, true),
    ROLE_ADMIN("ROLE_ADMIN", "Administrator - Manage users & content", 10, true),
    ROLE_MODERATOR("ROLE_MODERATOR", "Moderator - Manage content only", 50, false),
    ROLE_PREMIUM("ROLE_PREMIUM", "Premium User - Access premium features", 80, false),
    ROLE_USER("ROLE_USER", "Regular User - Basic access", 100, false);

    private final String name;
    private final String description;
    private final int priority;
    private final boolean isSystemRole;

    RoleEnum(String name, String description, int priority, boolean isSystemRole) {
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.isSystemRole = isSystemRole;
    }

    //    Lấy tất cả tên system roles
    public static Set<String> getSystemRoleNames() {
        return Arrays.stream(values()).filter(RoleEnum::isSystemRole).map(RoleEnum::getName).collect(Collectors.toSet());
    }

    //  Lấy tất cả tên roles
    public static Set<String> getAllRoleNames() {
        return Arrays.stream(values())
                .map(RoleEnum::getName)
                .collect(Collectors.toSet());
    }

    //    Check có phải system role không
    public static boolean isSystemRole(String roleName) {
        return Arrays.stream(values()).filter(RoleEnum::isSystemRole).anyMatch(r -> r.getName().equals(roleName));
    }


    public static RoleEnum fromName(String name) {
        for (RoleEnum role : values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role: " + name);
    }

    public boolean hasPrivilegeOver(RoleEnum other) {
        return this.priority <= other.priority;
    }
}
