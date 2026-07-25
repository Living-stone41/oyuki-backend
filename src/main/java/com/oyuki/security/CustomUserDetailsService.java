package com.oyuki.security;

import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String identifier
    ) throws UsernameNotFoundException {

        if (identifier == null ||
                identifier.isBlank()) {

            throw new UsernameNotFoundException(
                    "Email address or phone number is required"
            );
        }

        String cleanedIdentifier =
                identifier.trim();

        String emailIdentifier =
                cleanedIdentifier.toLowerCase(
                        Locale.ROOT
                );

        String phoneIdentifier =
                cleanedIdentifier
                        .replace(" ", "")
                        .replace("-", "");

        User user = userRepository
                .findByEmailIgnoreCaseOrPhoneNumber(
                        emailIdentifier,
                        phoneIdentifier
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                );

        boolean enabled =
                user.getStatus() ==
                        AccountStatus.ACTIVE ||
                user.getStatus() ==
                        AccountStatus.PENDING_APPROVAL;

        boolean accountNonLocked =
                user.getStatus() !=
                        AccountStatus.SUSPENDED &&
                user.getStatus() !=
                        AccountStatus.DISABLED;

        String username =
                user.getEmail() != null &&
                !user.getEmail().isBlank()
                        ? user.getEmail()
                        : user.getPhoneNumber();

        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(user.getPasswordHash())
                .authorities(List.of(authority))
                .accountExpired(false)
                .accountLocked(!accountNonLocked)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}