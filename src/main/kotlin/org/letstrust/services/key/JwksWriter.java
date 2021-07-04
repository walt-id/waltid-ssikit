package org.letstrust.services.key;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.crypto.tink.KeysetWriter;
import com.google.crypto.tink.proto.*;
import com.google.crypto.tink.proto.Keyset.Key;
import com.google.crypto.tink.subtle.EllipticCurves.CurveType;
import com.google.protobuf.ExtensionRegistry;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyOperation;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.crypto.tink.subtle.EllipticCurves.getEcPublicKey;
import static com.nimbusds.jose.jwk.Curve.P_256;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides utility functions for conversion of a Tink keyset to a JWKS (JSON Web Key Set).
 *
 * <p>Not all of the JWKS standard (specified in RFC 7517) is implemented. To write a keyset, all of
 * the keys in the given keyset must satisfy a few conditions:
 *
 * <ul>
 *   <li>All keys in the keyset must be public.
 *   <li>All keys in the keyset must have an {@code output_prefix_type} of {@code RAW}.
 * </ul>
 * <p>
 * In addition, this library only supports {@code ES256} keys. This means that all keys included in
 * the keyset must have a {@code hash_algorithm} of {@code SHA256}, a {@code curve} of {@code
 * NIST_P256}, and a {@code signature_encoding} of {@code IEEE_P1363}.
 */
public final class JwksWriter implements KeysetWriter {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String ECDSA_PUBLIC_KEY_URL =
            "type.googleapis.com/google.crypto.tink.EcdsaPublicKey";
    private static final EcdsaParams ES256_ECDSA_PARAMS =
            EcdsaParams.newBuilder()
                    .setHashType(HashType.SHA256)
                    .setCurve(EllipticCurveType.NIST_P256)
                    .setEncoding(EcdsaSignatureEncoding.IEEE_P1363)
                    .build();

    private final OutputStream outputStream;

    private JwksWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public static JwksWriter withOutputStream(OutputStream stream) {
        return new JwksWriter(stream);
    }

    private static JWK createJwk(Key key) throws IOException, GeneralSecurityException {
        if (key.getOutputPrefixType() != OutputPrefixType.RAW) {
            throw new InvalidKeySpecException(
                    String.format(
                            "Unsupported output_prefix_type for key_id %d: %s (want RAW)",
                            key.getKeyId(), key.getOutputPrefixType()));
        }

        switch (key.getKeyData().getTypeUrl()) {
            case ECDSA_PUBLIC_KEY_URL:
                return createEcdsaJwk(key);
            default:
                throw new InvalidKeySpecException(
                        String.format(
                                "Unsupported type_url for key_id %d: %s",
                                key.getKeyId(), key.getKeyData().getTypeUrl()));
        }
    }

    private static ECKey createEcdsaJwk(Key key) throws IOException, GeneralSecurityException {
        checkArgument(ECDSA_PUBLIC_KEY_URL.equals(key.getKeyData().getTypeUrl()));

        // Parse & validate EcdsaPublicKey.
        EcdsaPublicKey ecdsaPublicKey =
                EcdsaPublicKey.parseFrom(
                        key.getKeyData().getValue(), ExtensionRegistry.getEmptyRegistry());
        if (!ES256_ECDSA_PARAMS.equals(ecdsaPublicKey.getParams())) {
            throw new InvalidKeySpecException(
                    String.format(
                            "Unsupported ECDSA params for key_id %d: %s (want %s)",
                            key.getKeyId(), ecdsaPublicKey.getParams(), ES256_ECDSA_PARAMS));
        }

        // Convert to stdlib ECPublicKey, then to NimbusDS' ECKey.
        ECPublicKey publicKey =
                getEcPublicKey(
                        CurveType.NIST_P256,
                        ecdsaPublicKey.getX().toByteArray(),
                        ecdsaPublicKey.getY().toByteArray());
        return new ECKey.Builder(P_256, publicKey)
                .algorithm(JWSAlgorithm.ES256)
                .keyID(String.valueOf(key.getKeyId()))
                .keyOperations(ImmutableSet.of(KeyOperation.VERIFY))
                .build();
    }

    @Override
    public void write(Keyset keyset) throws IOException {
        JWKSet jwkSet;
        try {
            jwkSet = createJwks(keyset);
        } catch (GeneralSecurityException exception) {
            throw new IOException(exception);
        }
        outputStream.write(jwkSet.toString().getBytes(UTF_8));
    }

    @Override
    public void write(EncryptedKeyset keyset) throws IOException {
        throw new UnsupportedOperationException("JwksWriter.write(EncryptedKeyset) is unimplemented.");
    }

    public JWKSet createJwks(Keyset keyset) throws IOException, GeneralSecurityException {
        ImmutableList.Builder<JWK> jwksBuilder = ImmutableList.builder();
        for (Key key : keyset.getKeyList()) {
            if (key.getStatus() != KeyStatusType.ENABLED) {
                continue;
            }
            jwksBuilder.add(createJwk(key));
        }
        return new JWKSet(jwksBuilder.build());
    }
}
