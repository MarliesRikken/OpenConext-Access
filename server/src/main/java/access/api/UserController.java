package access.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static access.SwaggerOpenIdConfig.OPEN_ID_SCHEME_NAME;

@RestController
@RequestMapping(value = {"/api/v1/users", "/api/external/v1/users"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Transactional
@SecurityRequirement(name = OPEN_ID_SCHEME_NAME, scopes = {"openid"})
public class UserController {

    private static final Log LOG = LogFactory.getLog(UserController.class);

    @Autowired
    public UserController() {
    }

    @GetMapping("config")
    public ResponseEntity<Map<String, String>> config(Authentication authentication) {
        LOG.debug("/config");
        return ResponseEntity.ok(Map.of("res", "config"));
    }

    @GetMapping("me")
    public ResponseEntity<Map<String, String>> me(Authentication authentication) {
        LOG.debug("/me");
        return ResponseEntity.ok(Map.of("res", "me"));
    }

}
