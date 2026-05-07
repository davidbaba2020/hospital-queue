package com.hospital.queue.security;

import com.hospital.queue.domain.entity.User;
import com.hospital.queue.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean enabled;
    private final boolean locked;

    public CustomUserDetails(User user) {
        this.id       = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.email    = user.getEmail();
        this.role     = user.getRole();
        this.enabled  = user.isEnabled();
        this.locked   = user.isLocked();
    }

    public Long getId()        { return id; }
    public String getFullName(){ return fullName; }
    public String getEmail()   { return email; }
    public Role getRole()      { return role; }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return !locked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }
}
