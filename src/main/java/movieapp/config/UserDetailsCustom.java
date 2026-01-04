package movieapp.config;

import lombok.RequiredArgsConstructor;
import movieapp.repository.UserRepository;
import movieapp.service.UserService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component("userDetailsService")
@RequiredArgsConstructor
public class UserDetailsCustom implements UserDetailsService {
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 1. Gọi Service tìm user trong DB (username ở đây chính là email)
        movieapp.entity.User user = userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản với email/username: " + usernameOrEmail
                ));
//        movieapp.entity.User user = userService.handleFindUserByEmailEntity(username);
        if (!user.getIsActive())
            throw new DisabledException("Tài khoản đã bị khóa. Vui lòng liên hệ admin để được hỗ trợ.");


        if (user == null) throw new UsernameNotFoundException("User không tồn tại");

        if ("GOOGLE".equals(user.getProvider()) && user.getPassword() == null || user.getPassword().isEmpty())
            throw new UsernameNotFoundException("Tài khoản đăng ký bằng Google. Vui lòng đăng nhập bằng Google");

        Collection<GrantedAuthority> authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());

        // 2. Trả về User của Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                true,      // enabled
                true,                     // accountNonExpired
                true,                     // credentialsNonExpired
                true,                     // accountNonLocked
                authorities               // Roles từ database
        );
    }
}
