package com.campus.activity.security;

import com.campus.activity.user.entity.Permission;
import com.campus.activity.user.entity.Role;
import com.campus.activity.user.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class LoginUser implements UserDetails {

    private User user;
    private List<Role> roles;
    private List<Permission> permissions;
    private Collection<? extends GrantedAuthority> authorities;

    public LoginUser(User user, List<Role> roles, List<Permission> permissions) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
        
        this.authorities = Stream.concat(
                roles.stream().map(role -> new SimpleGrantedAuthority(role.getRoleCode())),
                permissions.stream().map(perm -> new SimpleGrantedAuthority(perm.getPermCode()))
        ).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}
