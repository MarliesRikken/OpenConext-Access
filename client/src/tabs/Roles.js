import "./Roles.scss";
import {useAppStore} from "../stores/AppStore";
import React, {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Button, Chip, Loader} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {AUTHORITIES, highestAuthority, isUserAllowed, markAndFilterRoles} from "../utils/UserRole";
import {rolesByApplication, searchRoles} from "../api";
import {isEmpty, stopEvent} from "../utils/Utils";
import debounce from "lodash.debounce";
import {chipTypeForUserRole} from "../utils/Authority";
import {ReactComponent as VoidImage} from "../icons/undraw_void_-3-ggu.svg";

export const Roles = () => {
    const user = useAppStore(state => state.user);
    const {roleSearchRequired} = useAppStore(state => state.config);
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [searching, setSearching] = useState(false);
    const [roles, setRoles] = useState([]);
    const [moreToShow, setMoreToShow] = useState(false);
    const [noResults, setNoResults] = useState(false);// eslint-disable-line no-unused-vars

    useEffect(() => {
        if (isUserAllowed(AUTHORITIES.MANAGER, user)) {
            if (!roleSearchRequired) {
                rolesByApplication()
                    .then(res => setRoles(markAndFilterRoles(user, res, I18n.locale)))
                setLoading(false);
            }
        } else {
            setRoles(markAndFilterRoles(user, [], I18n.locale))
        }
        setLoading(false);
    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps

    const openRole = (e, role) => {
        const id = role.isUserRole ? role.role.id : role.id;
        const path = `/roles/${id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const search = query => {
        if (!isEmpty(query) && query.trim().length > 2) {
            setSearching(true);
            delayedAutocomplete(query);
        }
        if (isEmpty(query)) {
            setSearching(false);
            setMoreToShow(false);
            setNoResults(false);
            setRoles([]);
        }
    };

    const delayedAutocomplete = debounce(query => {
        searchRoles(query)
            .then(res => {
                setRoles(markAndFilterRoles(user, res, I18n.locale));
                setMoreToShow(res.length === 15);
                setNoResults(res.length === 0);
                setSearching(false);
            });
    }, 500);

    const moreResultsAvailable = () => {
        return (
            <div className="more-results-available">
                <span>{I18n.t("users.moreResults")}</span>
            </div>)
    }

    const noRolesInstitutionAdmin = () => {
        const metaDataFields = user.institution.data.metaDataFields;
        const name = metaDataFields[`name:${I18n.locale}`] || metaDataFields["name:en"];
        const logo = metaDataFields["logo:0:url"]
        return (
            <div className="institution-admin-welcome">
                {logo ? <img src={logo} alt="logo"/> : <VoidImage/>}
                <p>{I18n.t("institutionAdmin.welcome", {name: name})}</p>
                <Button txt={I18n.t("institutionAdmin.create")} onClick={() => navigate("/role/new")}/>
            </div>
        );
    }

    const columns = [
        {
            nonSortable: true,
            key: "logo",
            header: "",
            mapper: role => {
                return <div className="role-icon">
                    <img src={role.logo} alt="logo"/>
                </div>
            }
        },
        {
            key: "applicationName",
            header: I18n.t("roles.applicationName"),
            mapper: role => <span>{role.applicationName}</span>
        },
        {
            key: "name",
            header: I18n.t("roles.accessRole"),
            mapper: role => <span>{role.name}</span>
        },
        {
            key: "description",
            header: I18n.t("roles.description"),
            mapper: role => <span className={"cut-of-lines"}>{role.description}</span>
        },
        {
            key: "authority",
            header: I18n.t("roles.authority"),
            mapper: role => <Chip type={chipTypeForUserRole(role.authority)}
                                  label={role.isUserRole ? I18n.t(`access.${role.authority}`) :
                                      I18n.t("roles.noMember")}/>
        },
        {
            key: "userRoleCount",
            header: I18n.t("roles.userRoleCount"),
            mapper: role => role.userRoleCount
        }

    ];

    if (loading) {
        return <Loader/>
    }

    const isSuperUser = isUserAllowed(AUTHORITIES.SUPER_USER, user);
    const isManager = isUserAllowed(AUTHORITIES.MANAGER, user);
    const isInstitutionAdmin = highestAuthority(user) === AUTHORITIES.INSTITUTION_ADMIN;
    if (isInstitutionAdmin && !isEmpty(user.institution)) {
        return (
            <div className={"mod-roles"}>
                {noRolesInstitutionAdmin()}
            </div>
        )
    }

    return (
        <div className={"mod-roles"}>
            <Entities
                entities={isSuperUser ? roles : roles.filter(role => !(role.isUserRole && role.authority === "GUEST"))}
                modelName="roles"
                showNew={isManager}
                newLabel={I18n.t("roles.new")}
                newEntityPath={"/role/new"}
                defaultSort="name"
                columns={columns}
                searchAttributes={["name", "description", "applicationName"]}
                customNoEntities={I18n.t(`roles.noResults`)}
                loading={false}
                inputFocus={true}
                hideTitle={false}
                filters={moreToShow && moreResultsAvailable()}
                customSearch={roleSearchRequired && isSuperUser ? search : null}
                rowLinkMapper={isUserAllowed(AUTHORITIES.INVITER, user) ? openRole : null}
                busy={searching}/>
        </div>
    );

}
