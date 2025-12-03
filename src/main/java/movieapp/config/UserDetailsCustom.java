package movieapp.config;

import lombok.RequiredArgsConstructor;
import movieapp.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("userDetailsService")
@RequiredArgsConstructor
public class UserDetailsCustom implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Gọi Service tìm user trong DB (username ở đây chính là email)
        movieapp.domain.User user;
        try {
            user = userService.handleFindUserByEmailEntity(username);
        } catch (Exception e) {
            throw new UsernameNotFoundException("Username/password không hợp lệ");
        }
        // 2. Trả về User của Spring Security
        return new User(user.getEmail(), user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
