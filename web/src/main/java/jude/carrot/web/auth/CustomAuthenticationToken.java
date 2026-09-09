package jude.carrot.web.auth;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public CustomAuthenticationToken(CustomUserDetails principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

    @Override
    public Object getPrincipal() {
        CustomUserDetails userDetails = (CustomUserDetails) super.getPrincipal();
        if(userDetails == null) return null;
        return Long.parseLong(userDetails.getUsername());
    }
}