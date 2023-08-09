package de.panomenal.core.gateway.data;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class ConnValidationResponse {
    private String status;
    private boolean isAuthenticated;
    private String username;
    private String token;
    private List<String> authorities;
}
