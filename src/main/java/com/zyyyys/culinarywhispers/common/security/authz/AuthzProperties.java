package com.zyyyys.culinarywhispers.common.security.authz;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "cw.authz")
public class AuthzProperties {
    private List<String> admins = new ArrayList<>();
    private List<String> superadmins = new ArrayList<>();
}
