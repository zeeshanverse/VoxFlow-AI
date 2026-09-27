package com.voxflow.profile;
import com.voxflow.common.CurrentUser;
import com.voxflow.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/profile")
public class ProfileController {
    private final CurrentUser current; private final UserRepository users;
    public ProfileController(CurrentUser current,UserRepository users){this.current=current;this.users=users;}
    @GetMapping public Profile get(){var u=current.get();return new Profile(u.getId(),u.getEmail(),u.getDisplayName());}
    @PutMapping public Profile update(@Valid @RequestBody Update r){var u=current.get();u.setDisplayName(r.displayName());users.save(u);return new Profile(u.getId(),u.getEmail(),u.getDisplayName());}
    public record Profile(Long id,String email,String displayName){}
    public record Update(@NotBlank String displayName){}
}
