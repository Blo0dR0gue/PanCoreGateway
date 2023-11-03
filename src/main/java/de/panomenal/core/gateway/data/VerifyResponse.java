package de.panomenal.core.gateway.data;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class VerifyResponse {
    private String status;
    private boolean authenticated;
    private String username;
    private List<Authorities> authorities;
}
