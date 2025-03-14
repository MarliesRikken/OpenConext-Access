package access.api;

import access.config.Config;
import access.exception.NotFoundException;
import access.manage.ManageIdentifier;
import access.manage.Manage;
import access.model.Authority;
import access.model.User;
import access.repository.UserRepository;
import access.security.UserPermissions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/users", "/api/external/v1/users"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
@EnableConfigurationProperties(Config.class)
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    private final Config config;
    private final UserRepository userRepository;
    private final Manage manage;
    private final ObjectMapper objectMapper;


    @Autowired
    public UserController(Config config,
                          UserRepository userRepository,
                          Manage manage,
                          ObjectMapper objectMapper,
                          @Value("${voot.group_urn_domain}") String groupUrnPrefix) {
        this.config = config.withGroupUrnPrefix(groupUrnPrefix);
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.manage = manage;
    }

    @GetMapping("config")
    public ResponseEntity<Config> config(User user) {
        LOG.debug("/config");
        Config result = new Config(this.config);
        result
                .withAuthenticated(user != null && user.getId() != null)
                .withName(user != null ? user.getName() : null);
        if (user != null && user.getId() == null) {
            verifyMissingAttributes(user, result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("me")
    public ResponseEntity<User> me(@Parameter(hidden = true) User user) {
        LOG.debug("/me");
        List<Map<String, Object>> providers = getProviders(user);
        user.setProviders(providers);

        return ResponseEntity.ok(user);
    }

    @GetMapping("other/{id}")
    public ResponseEntity<User> details(@PathVariable("id") Long id, @Parameter(hidden = true) User user) {
        LOG.debug("/me");
        UserPermissions.assertSuperUser(user);
        User other = userRepository.findById(id).orElseThrow(NotFoundException::new);
        List<Map<String, Object>> providers = getProviders(other);
        other.setProviders(providers);

        return ResponseEntity.ok(other);
    }

    @GetMapping("search")
    public ResponseEntity<List<User>> search(@RequestParam(value = "query") String query,
                                             @Parameter(hidden = true) User user) {
        LOG.debug("/search");
        UserPermissions.assertSuperUser(user);
        List<User> users = query.equals("owl") ? userRepository.findAll() :
                userRepository.search(query.replaceAll("@", " ") + "*", 15);
        return ResponseEntity.ok(users);
    }

    @GetMapping("login")
    public View login(@RequestParam(value = "app", required = false, defaultValue = "client") String app) {
        LOG.debug("/login");
        return new RedirectView(app.equals("client") ? config.getClientUrl() : config.getWelcomeUrl(), false);
    }

    @GetMapping("logout")
    public ResponseEntity<Map<String, Integer>> logout(HttpServletRequest request) {
        LOG.debug("/logout");
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Results.okResult();
    }

    @GetMapping("switch")
    public View switchApp(@Param(value = "app") String app, @Parameter(hidden = true) User user) {
        boolean welcome = app.equals("welcome");
        UserPermissions.assertAuthority(user, welcome ? Authority.GUEST : Authority.INVITER);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        //This will force a cookie to authenticate
        securityContext.setAuthentication(authentication);
        return new RedirectView(welcome ? config.getWelcomeUrl() : config.getClientUrl(), false);
    }

    @PostMapping("error")
    public ResponseEntity<Map<String, Integer>> error(@RequestBody Map<String, Object> payload,
                                                      @Parameter(hidden = true) User user) throws
            JsonProcessingException, UnknownHostException {
        payload.put("dateTime", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        payload.put("machine", InetAddress.getLocalHost().getHostName());
        payload.put("user", user);
        String msg = objectMapper.writeValueAsString(payload);
        LOG.error(msg, new IllegalArgumentException(msg));
        return Results.createResult();
    }

    private List<Map<String, Object>> getProviders(User user) {
        return user.getUserRoles().stream()
                .map(userRole -> new ManageIdentifier(userRole.getRole().getManageId(), userRole.getRole().getManageType()))
                //Prevent unnecessary round-trips to Manage
                .collect(Collectors.toSet())
                .stream()
                .map(identity -> manage.providerById(identity.entityType(), identity.id()))
                .toList();
    }

    private void verifyMissingAttributes(User user, Config result) {
        List<String> missingAttributes = new ArrayList<>();
        if (!StringUtils.hasText(user.getSub())) {
            missingAttributes.add("sub");
        }
        if (!StringUtils.hasText(user.getEduPersonPrincipalName())) {
            missingAttributes.add("eduPersonPrincipalName");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            missingAttributes.add("email");
        }
        if (!StringUtils.hasText(user.getSchacHomeOrganization())) {
            missingAttributes.add("schacHomeOrganization");
        }
        if (!missingAttributes.isEmpty()) {
            result.withMissingAttributes(missingAttributes);
        }
    }


}
