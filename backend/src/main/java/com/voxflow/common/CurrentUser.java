package com.voxflow.common;

import com.voxflow.user.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users){this.users=users;}
    public User get(){
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || auth.getName()==null) throw new IllegalStateException("Authentication required");
        return users.findByEmail(auth.getName()).orElseThrow();
    }
}
