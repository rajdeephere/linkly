package com.linkly.auth;

import com.linkly.auth.dto.AuthDtos.AuthResponse;
import com.linkly.auth.dto.AuthDtos.LoginRequest;
import com.linkly.auth.dto.AuthDtos.RegisterRequest;
import com.linkly.user.User;
import com.linkly.user.UserRepository;
import com.linkly.workspace.Membership;
import com.linkly.workspace.MembershipRepository;
import com.linkly.workspace.Workspace;
import com.linkly.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final MembershipRepository memberships;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, WorkspaceRepository workspaces,
                       MembershipRepository memberships, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.workspaces = workspaces;
        this.memberships = memberships;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Register → create user + their own workspace + an owner membership → issue a token. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setName(req.name());
        user.setPasswordHash(encoder.encode(req.password()));
        user = users.save(user);

        Workspace ws = new Workspace();
        ws.setName((req.name() == null || req.name().isBlank() ? email : req.name()) + "'s workspace");
        ws = workspaces.save(ws);

        Membership m = new Membership();
        m.setWorkspaceId(ws.getId());
        m.setUserId(user.getId());
        m.setRole("owner");
        memberships.save(m);

        return token(user, ws.getId(), "owner");
    }

    /** Login → verify password → issue a token for the user's workspace. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase()).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !encoder.matches(req.password(), user.getPasswordHash())) {
            // Same response whether the user is unknown or the password is wrong (no enumeration).
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
        }
        Membership m = memberships.findFirstByUserId(user.getId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "no workspace membership"));
        return token(user, m.getWorkspaceId(), m.getRole());
    }

    private AuthResponse token(User user, java.util.UUID workspaceId, String role) {
        String jwtToken = jwt.issue(user.getId(), workspaceId, role, user.getEmail());
        return new AuthResponse(jwtToken, user.getId(), workspaceId, role, user.getEmail());
    }
}
