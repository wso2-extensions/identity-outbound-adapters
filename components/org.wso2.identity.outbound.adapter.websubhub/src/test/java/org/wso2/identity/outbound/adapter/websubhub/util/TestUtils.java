package org.wso2.identity.outbound.adapter.websubhub.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ASYMMETRIC_ENCRYPTION_ALGORITHM;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.CRYPTO_KEY_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.CRYPTO_KEY_RESPONSE_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.ENCRYPTED_PAYLOAD_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.IV_PARAMETER_SPEC_JSON_KEY;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.SYMMETRIC_ENCRYPTION_ALGORITHM;
import static org.wso2.identity.outbound.adapter.websubhub.util.WebSubHubAdapterConstants.SYMMETRIC_ENCRYPTION_ALGORITHM_WITH_MODE;

/**
 * Common utility methods for tests.
 */
public class TestUtils {

    private static final String PUBLIC_KEY_FILE_PATH = "src/test/resources/crypto-public-key.json";
    private static final String PRIVATE_KEY_FILE_PATH = "src/test/resources/crypto-private-key.json";

    /**
     * Read and return encryption public key response from resource file.
     *
     * @return JSONObject containing a mocked public key API response.
     * @throws IOException    If an error occurs while reading the file.
     * @throws ParseException If an error occurs while parsing.
     */
    public static JSONObject getCryptoPublicKey() throws IOException, ParseException {

        String resourceFilePath = new File(PUBLIC_KEY_FILE_PATH).getAbsolutePath();
        JSONParser jsonParser = new JSONParser();
        JSONObject keyResponseJSON = (JSONObject) jsonParser.parse(
                new InputStreamReader(Files.newInputStream(Paths.get(resourceFilePath)), StandardCharsets.UTF_8));
        return keyResponseJSON;
    }

    /**
     * Read and return decryption private key response from resource file.
     *
     * @return PrivateKey object.
     * @throws IOException      If an error occurs while reading the file.
     * @throws ParseException   If an error occurs while parsing.
     */
    public static PrivateKey getCryptoPrivateKey() throws IOException, ParseException, NoSuchAlgorithmException,
            InvalidKeySpecException {

        String resourceFilePath = new File(PRIVATE_KEY_FILE_PATH).getAbsolutePath();
        JSONParser jsonParser = new JSONParser();
        JSONObject keyResponseJSON = (JSONObject) jsonParser.parse(
                new InputStreamReader(Files.newInputStream(Paths.get(resourceFilePath)), StandardCharsets.UTF_8));
        byte[] pkcs8EncodedBytes =
                Base64.getDecoder().decode(keyResponseJSON.get(CRYPTO_KEY_RESPONSE_JSON_KEY).toString());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance(ASYMMETRIC_ENCRYPTION_ALGORITHM);
        PrivateKey privateKey = kf.generatePrivate(keySpec);
        return privateKey;
    }

    /**
     * Decrypts and constructs the event payload in the proper format.
     *
     * @param eventPayloadJSONString  Encrypted event payload string.
     * @return                        Decrypted event payload.
     * @throws Exception Error while decrypting and constructing event payload.
     */
    public static JSONObject decryptEventPayload(String eventPayloadJSONString) throws Exception {

        Cipher decryptCipher = Cipher.getInstance(ASYMMETRIC_ENCRYPTION_ALGORITHM);
        PrivateKey rsaPrivateKey =  getCryptoPrivateKey();
        decryptCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);

        JSONParser jsonParser = new JSONParser();
        JSONObject eventPayloadJSON = (JSONObject) jsonParser.parse(
                eventPayloadJSONString);
        byte[] decodedSymmetricKeyBytes =
                Base64.getDecoder().decode(eventPayloadJSON.get(CRYPTO_KEY_JSON_KEY).toString());
        byte[] decryptedMessageBytes = decryptCipher.doFinal(decodedSymmetricKeyBytes);
        SecretKey decryptedSymmetricKey = new SecretKeySpec(
                decryptedMessageBytes, 0, decryptedMessageBytes.length, SYMMETRIC_ENCRYPTION_ALGORITHM);

        String encryptedEventPayload = eventPayloadJSON.get(ENCRYPTED_PAYLOAD_JSON_KEY).toString();
        byte[] ivParameterSpec =
                Base64.getDecoder().decode(eventPayloadJSON.get(IV_PARAMETER_SPEC_JSON_KEY).toString());
        byte[] dataInBytes = Base64.getDecoder().decode(encryptedEventPayload);
        Cipher decryptionCipher = Cipher.getInstance(SYMMETRIC_ENCRYPTION_ALGORITHM_WITH_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(128, ivParameterSpec);
        decryptionCipher.init(Cipher.DECRYPT_MODE, decryptedSymmetricKey, spec);
        byte[] decryptedPayloadBytes = decryptionCipher.doFinal(dataInBytes);
        return (JSONObject) jsonParser.parse(new String(decryptedPayloadBytes));
    }
}
