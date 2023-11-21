package access.model;

import access.Seed;
import access.manage.EntityType;
import access.manage.ManageIdentifier;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void groupBy() {
        List<Role> roles = List.of(
                new Role("cloud", "cloud", "https://landingpage.com", Seed.application("1", EntityType.OIDC10_RP), 365, false, false),
                new Role("mail", "mail", "https://landingpage.com", Seed.application("1", EntityType.OIDC10_RP), 365, false, false),
                new Role("wiki", "wiki", "https://landingpage.com", Seed.application("2", EntityType.SAML20_SP), 365, false, false)
        );
        Set<ManageIdentifier> rolesByApplication = roles.stream()
                .map(Role::getApplications)
                .flatMap(Collection::stream)
                .map(application -> new ManageIdentifier(application.getManageId(), application.getManageType()))
                .collect(Collectors.toSet());

        assertEquals(2, rolesByApplication.size());
    }
}