package access.model;

import access.manage.ManageIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static access.security.InstitutionAdmin.*;

@Entity(name = "users")
@NoArgsConstructor
@Getter
@Setter
@SuppressWarnings("unchecked")
public class User implements Serializable, Provisionable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull
    private String sub;

    @Column(name = "super_user")
    @NotNull
    private boolean superUser;

    @Column(name = "eduperson_principal_name")
    @NotNull
    private String eduPersonPrincipalName;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "name")
    private String name;

    @Column(name = "schac_home_organization")
    private String schacHomeOrganization;

    @Column(name = "organization_guid")
    private String organizationGUID;

    @Column(name = "institution_admin")
    @NotNull
    private boolean institutionAdmin;

    @Column
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_activity")
    private Instant lastActivity;

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "user", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RemoteProvisionedUser> remoteProvisionedUsers = new HashSet<>();

    @Transient
    private List<Map<String, Object>> applications = Collections.emptyList();

    @Transient
    private Map<String, Object> institution = Collections.emptyMap();

    public User(Map<String, Object> attributes) {
        this(false, attributes);
    }

    public User(boolean superUser, Map<String, Object> attributes) {
        this.superUser = superUser;
        this.sub = (String) attributes.get("sub");
        this.eduPersonPrincipalName = (String) attributes.get("eduperson_principal_name");
        this.schacHomeOrganization = (String) attributes.get("schac_home_organization");
        this.email = (String) attributes.get("email");
        this.givenName = (String) attributes.get("given_name");
        this.familyName = (String) attributes.get("family_name");
        this.institutionAdmin = (boolean) attributes.getOrDefault(INSTITUTION_ADMIN, false);
        this.organizationGUID = (String) attributes.get(ORGANIZATION_GUID);
        this.applications = (List<Map<String, Object>>) attributes.getOrDefault(APPLICATIONS, Collections.emptyList());
        this.institution = (Map<String, Object>) attributes.getOrDefault(INSTITUTION, Collections.emptyMap());
        this.createdAt = Instant.now();
        this.lastActivity = this.createdAt;

        String name = (String) attributes.get("name");
        String preferredUsername = (String) attributes.get("preferred_username");
        if (StringUtils.hasText(name)) {
            this.name = name;
        } else if (StringUtils.hasText(preferredUsername)) {
            this.name = preferredUsername;
        } else if (StringUtils.hasText(this.givenName) && StringUtils.hasText(this.familyName)) {
            this.name = this.givenName + " " + this.familyName;
        } else if (StringUtils.hasText(this.email)) {
            this.name = Stream.of(this.email.substring(0, this.email.indexOf("@")).toLowerCase().split("\\."))
                    .map(StringUtils::capitalize)
                    .collect(Collectors.joining(" "));
        } else if (StringUtils.hasText(this.sub)) {
            this.name = StringUtils.capitalize(this.sub.substring(this.sub.lastIndexOf(":") + 1));
        }
        if (!StringUtils.hasText(this.givenName) &&
                !StringUtils.hasText(this.familyName) &&
                StringUtils.hasText(this.name) &&
                this.name.contains(" ")) {
            String[] splittedName = this.name.split(" ", 2);
            this.givenName = splittedName[0];
            this.familyName = splittedName[1];
        }
    }

    public User(boolean superUser, String eppn, String sub, String schacHomeOrganization, String givenName, String familyName, String email) {
        this.superUser = superUser;
        this.eduPersonPrincipalName = eppn;
        this.sub = sub;
        this.schacHomeOrganization = schacHomeOrganization;
        this.givenName = givenName;
        this.familyName = familyName;
        this.name = String.format("%s %s", givenName, familyName);
        this.email = email;
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
    }

    @JsonIgnore
    public void addUserRole(UserRole userRole) {
        this.userRoles.add(userRole);
        userRole.setUser(this);
    }

    @JsonIgnore
    public void removeUserRole(UserRole role) {
        //This is required by Hibernate - children can't be de-referenced
        Set<UserRole> newRoles = userRoles.stream().filter(ur -> !ur.getId().equals(role.getId())).collect(Collectors.toSet());
        userRoles.clear();
        userRoles.addAll(newRoles);
    }

    @JsonIgnore
    public Set<ManageIdentifier> manageIdentifierSet() {
        return userRoles.stream()
                .filter(userRole -> userRole.getAuthority().equals(Authority.GUEST))
                .map(userRole -> new ManageIdentifier(userRole.getRole().getManageId(), userRole.getRole().getManageType()))
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public Map<String, Object> asMap() {
        return Map.of(
                "id", id,
                "name", StringUtils.hasText(name) ? name : email,
                "email", email,
                "createdAt", createdAt,
                "lastActivity", lastActivity,
                "schacHomeOrganization", schacHomeOrganization,
                "sub", sub
        );
    }

    @JsonIgnore
    public void updateAttributes(Map<String, Object> attributes) {
        this.eduPersonPrincipalName = (String) attributes.get("eduperson_principal_name");
        this.schacHomeOrganization = (String) attributes.get("schac_home_organization");
        this.givenName = (String) attributes.get("given_name");
        this.familyName = (String) attributes.get("family_name");
        this.email = (String) attributes.get("email");
        this.institutionAdmin = (boolean) attributes.getOrDefault(INSTITUTION_ADMIN, false);
        this.organizationGUID = (String) attributes.get(ORGANIZATION_GUID);
        this.applications = (List<Map<String, Object>>) attributes.getOrDefault(APPLICATIONS, Collections.emptyList());
        this.institution = (Map<String, Object>) attributes.getOrDefault(INSTITUTION, Collections.emptyMap());
        this.lastActivity = Instant.now();
    }

}
