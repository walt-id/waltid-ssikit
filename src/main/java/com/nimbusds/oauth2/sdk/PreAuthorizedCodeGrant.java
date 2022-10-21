package com.nimbusds.oauth2.sdk;

import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.util.MultivaluedMapUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreAuthorizedCodeGrant extends AuthorizationGrant {
    public static final GrantType GRANT_TYPE = new GrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
    private final AuthorizationCode code;
    private final URI redirectURI;
    private final String userPin;
    private final CodeVerifier codeVerifier;

    public PreAuthorizedCodeGrant(final AuthorizationCode code,
                                  final URI redirectURI,
                                  final String userPin,
                                  final CodeVerifier codeVerifier) {
        super(GRANT_TYPE);
        if (code == null)
            throw new IllegalArgumentException("The authorisation code must not be null");
        this.code = code;
        this.redirectURI = redirectURI;
        this.userPin = userPin;
        this.codeVerifier = codeVerifier;
    }

    public AuthorizationCode getCode() {
        return code;
    }

    public URI getRedirectURI() {
        return redirectURI;
    }

    public String getUserPin() {
        return userPin;
    }

    public CodeVerifier getCodeVerifier() {
        return codeVerifier;
    }

    @Override
    public Map<String, List<String>> toParameters() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("grant_type", Collections.singletonList(GRANT_TYPE.getValue()));
        params.put("pre-authorized_code", Collections.singletonList(code.getValue()));

        if (redirectURI != null)
            params.put("redirect_uri", Collections.singletonList(redirectURI.toString()));

        if (userPin != null)
            params.put("user_pin", Collections.singletonList(userPin));

        if (codeVerifier != null)
            params.put("code_verifier", Collections.singletonList(codeVerifier.getValue()));

        return params;
    }

    public static PreAuthorizedCodeGrant parse(final Map<String, List<String>> params)
            throws ParseException {

        GrantType.ensure(GRANT_TYPE, params);

        // Parse authorisation code
        String codeString = MultivaluedMapUtils.getFirstValue(params, "pre-authorized_code");

        if (codeString == null || codeString.trim().isEmpty()) {
            String msg = "Missing or empty code parameter";
            throw new ParseException(msg, OAuth2Error.INVALID_REQUEST.appendDescription(": " + msg));
        }

        AuthorizationCode code = new AuthorizationCode(codeString);

        // Parse optional redirection URI
        String redirectURIString = MultivaluedMapUtils.getFirstValue(params, "redirect_uri");

        URI redirectURI = null;

        if (redirectURIString != null) {
            try {
                redirectURI = new URI(redirectURIString);
            } catch (URISyntaxException e) {
                String msg = "Invalid redirect_uri parameter: " + e.getMessage();
                throw new ParseException(msg, OAuth2Error.INVALID_REQUEST.appendDescription(": " + msg), e);
            }
        }

        var userPin = MultivaluedMapUtils.getFirstValue(params, "user_pin");

        // Parse optional code verifier
        String codeVerifierString = MultivaluedMapUtils.getFirstValue(params, "code_verifier");

        CodeVerifier codeVerifier = null;

        if (StringUtils.isNotBlank(codeVerifierString)) {

            try {
                codeVerifier = new CodeVerifier(codeVerifierString);
            } catch (IllegalArgumentException e) {
                // Illegal code verifier
                String msg = "Illegal code verifier: " + e.getMessage();
                throw new ParseException(e.getMessage(), OAuth2Error.INVALID_REQUEST.appendDescription(": " + msg), e);
            }
        }

        return new PreAuthorizedCodeGrant(code, redirectURI, userPin, codeVerifier);
    }
}
