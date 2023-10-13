package access.provision.graph;

import access.provision.ProvisioningResponse;

public record GraphResponse(String remoteIdentifier, String inviteRedeemUrl) implements ProvisioningResponse {

    @Override
    public boolean isGraphResponse() {
        return true;
    }
}
