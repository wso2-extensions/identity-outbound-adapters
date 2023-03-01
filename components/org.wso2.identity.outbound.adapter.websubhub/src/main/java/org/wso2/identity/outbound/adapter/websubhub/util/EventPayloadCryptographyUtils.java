/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.identity.outbound.adapter.websubhub.util;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import org.wso2.identity.outbound.adapter.websubhub.internal.WebSubHubAdapterDataHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.identity.event.IdentityEventException;
import sun.security.rsa.RSAPublicKeyImpl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ASYMMETRIC_ENCRYPTION_ALGORITHM;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.CRYPTO_KEY_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.CRYPTO_KEY_RESPONSE_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ENCRYPTED_PAYLOAD_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ENCRYPTION_KEY_ENDPOINT_URL_TENANT_PLACEHOLDER;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.IV_PARAMETER_SPEC_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.SYMMETRIC_ENCRYPTION_ALGORITHM;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.SYMMETRIC_ENCRYPTION_ALGORITHM_WITH_MODE;



/**
 * This class contains utility methods for encrypting Event Payloads.
 */
public class EventPayloadCryptographyUtils {

    private static final Log log = LogFactory.getLog(EventPayloadCryptographyUtils.class);
    private static final ConcurrentMap<String, DefaultJWKSetCache> cacheMap = new ConcurrentHashMap<>();
    private static final KeyGenerator keyGenerator;

    static {
        try {
            int keyBitLength = EncryptionMethod.A128GCM.cekBitLength();
            keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ENCRYPTION_ALGORITHM);
            keyGenerator.init(keyBitLength);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to initialize Event Payload Cryptographer.", e);
        }
    }

    /**
     * Encrypts the event payload and returns a JSON with the encrypted content.
     *
     * @param payloadJsonString Event payload JSON as a string.
     * @param tenantDomain      Tenant domain.
     * @return                  Encrypted event payload.
     * @throws IdentityEventException   Error while encrypting the JSON payload.
     */
    public static JSONObject encryptEventPayload(String payloadJsonString, String tenantDomain)
            throws IdentityEventException {

        try {
            // Encrypt event payload with symmetric encryption.
            SecretKey symmetricKey = keyGenerator.generateKey();
            Cipher symmetricEncryptionCipher = Cipher.getInstance(SYMMETRIC_ENCRYPTION_ALGORITHM_WITH_MODE);
            symmetricEncryptionCipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedBytes = symmetricEncryptionCipher
                    .doFinal(payloadJsonString.getBytes(StandardCharsets.UTF_8));
            String encryptedEventPayload = Base64.getEncoder().encodeToString(encryptedBytes);

            // Encrypt symmetric encryption key with asymmetric encryption.
            PublicKey publicKey = getPublicKey(tenantDomain);
            Cipher asymmetricEncryptionCipher = Cipher.getInstance(ASYMMETRIC_ENCRYPTION_ALGORITHM);
            asymmetricEncryptionCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedSymmetricKeyBytes = asymmetricEncryptionCipher.doFinal(symmetricKey.getEncoded());
            String encryptedSymmetricKey = Base64.getEncoder().encodeToString(encryptedSymmetricKeyBytes);

            JSONObject encryptedPayload = new JSONObject();
            encryptedPayload.put(ENCRYPTED_PAYLOAD_JSON_KEY, encryptedEventPayload);
            encryptedPayload.put(CRYPTO_KEY_JSON_KEY, encryptedSymmetricKey);
            encryptedPayload.put(IV_PARAMETER_SPEC_JSON_KEY,
                    Base64.getEncoder().encodeToString(symmetricEncryptionCipher.getIV()));
            return encryptedPayload;
        } catch (JOSEException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |
                 IllegalBlockSizeException | BadPaddingException | ParseException e) {
            throw new IdentityEventException("Error while encrypting event payload.", e);
        }
    }

    private static PublicKey getPublicKey(String tenantDomain) throws ParseException, InvalidKeyException,
            IdentityEventException, JOSEException {

        try {
            DefaultJWKSetCache orgJWKCache = getJWKCache(tenantDomain);
            if (orgJWKCache.get() != null && !orgJWKCache.isExpired()) {
                return convertJWKToPublicKey(orgJWKCache.get());
            }
            synchronized (orgJWKCache) {
                // Recheck cache for a valid key once the lock is obtained as another thread could have updated the
                // cache while this was waiting.
                if (orgJWKCache.get() != null && !orgJWKCache.isExpired()) {
                    return convertJWKToPublicKey(orgJWKCache.get());
                }

                Resource keyResource = retrieveKeyFromAPI(tenantDomain);
                JSONParser jsonParser = new JSONParser();
                JSONObject responseJSON = (JSONObject) jsonParser.parse(keyResource.getContent());
                if (responseJSON.get(CRYPTO_KEY_RESPONSE_JSON_KEY) == null) {
                    throw new IdentityEventException("Event encryption public key endpoint has returned an " +
                            "invalid response.");
                }
                byte[] publicKeyBytes =
                        Base64.getDecoder().decode(responseJSON.get(CRYPTO_KEY_RESPONSE_JSON_KEY).toString());
                RSAPublicKey rsaPublicKey = RSAPublicKeyImpl.newKey(publicKeyBytes);
                RSAKey compositePublicKeyJWK = new RSAKey(Base64URL.encode(rsaPublicKey.getModulus()),
                        Base64URL.encode(rsaPublicKey.getPublicExponent()), null, null, null, null, null,
                        null, null, null, KeyUse.ENCRYPTION, null, JWEAlgorithm.RSA_OAEP_256,
                        null, null, null, null, null, null);
                JWKSet jwkSet = new JWKSet(Collections.singletonList(compositePublicKeyJWK));
                orgJWKCache.put(jwkSet);
                return convertJWKToPublicKey(jwkSet);
            }
        } catch (IOException e) {
            throw new IdentityEventException("Unable to fetch event encryption public key for tenant " +
                    tenantDomain, e);
        }
    }

    private static DefaultJWKSetCache getJWKCache(String tenantDomain) {

        if (cacheMap.get(tenantDomain) == null) {
            int cacheLifeSpan = WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                    .getEncryptionKeyCacheLifespan();
            cacheMap.putIfAbsent(tenantDomain, new DefaultJWKSetCache(cacheLifeSpan, TimeUnit.MINUTES));
        }
        return cacheMap.get(tenantDomain);
    }

    private static PublicKey convertJWKToPublicKey(JWKSet jwkSet) throws JOSEException, IdentityEventException {

        JWK jwk = jwkSet.getKeys().get(0);
        if (jwk instanceof RSAKey) {
            RSAKey rsaKey = (RSAKey) jwk;
            return rsaKey.toPublicKey();
        } else {
            throw new IdentityEventException("Event encryption public key is not in RSA format. " +
                    "Unable to encrypt event.");
        }
    }

    private static Resource retrieveKeyFromAPI(String tenantDomain) throws IOException, IdentityEventException {

        // Retrieve key from the encryption key endpoint.
        DefaultResourceRetriever resourceRetriever =
                WebSubHubAdapterDataHolder.getInstance().getResourceRetriever();
        String keyEndpointURLString = StringUtils.replace(
                WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getEncryptionKeyEndpointUrl(),
                ENCRYPTION_KEY_ENDPOINT_URL_TENANT_PLACEHOLDER, tenantDomain);
        if (keyEndpointURLString == null) {
            throw new IdentityEventException("Event encryption public key endpoint URL is not configured.");
        }
        URL keyEndpointURL = new URL(keyEndpointURLString);
        return resourceRetriever.retrieveResource(keyEndpointURL);
    }
}
