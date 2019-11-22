package org.literacybridge.acm.cloud;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.lang3.tuple.Triple;
import org.json.simple.JSONObject;
import org.literacybridge.acm.cloud.cognito.AuthenticationHelper;
import org.literacybridge.acm.cloud.cognito.CognitoHelper;
import org.literacybridge.acm.cloud.cognito.CognitoJWTParser;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.HttpUtility;

import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Helper class to provide a simpler interface to cognito authentication.
 */
public class Authenticator {
    private static Authenticator instance;
    private static AmazonS3 s3Client = null;

    public static synchronized Authenticator getInstance() {
        if (instance == null) {
            instance = new Authenticator();
        }
        return instance;
    }

    private AuthenticationHelper.AuthenticationResult authenticationResult;
    private CognitoHelper cognitoHelper;
    private JSONObject authenticationInfo;
    private Credentials credentials;


    private String userName;
    private String userEmail;

    // Cached helpers. Ones not used internally are lazy allocated.
    private IdentityPersistence identityPersistence = new IdentityPersistence();
    private TbSrnHelper tbSrnHelper = null;
    private ProjectsHelper projectsHelper = null;

    private Authenticator() {
        cognitoHelper = new CognitoHelper();
    }

    static AmazonS3 getS3Client(Credentials credentials) {
        if (s3Client == null) {
            BasicSessionCredentials awsCreds = new BasicSessionCredentials(credentials.getAccessKeyId(),
                credentials.getSecretKey(),
                credentials.getSessionToken());
            s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
        }
        return s3Client;
    }

    AmazonS3 getS3Client() {
        return getS3Client(credentials);
    }

    void authenticate(String username, String password) {
        authenticationResult = cognitoHelper.ValidateUser(username, password);
        String jwtToken = authenticationResult.getJwtToken();
        if (jwtToken != null) {
            authenticationInfo = CognitoJWTParser.getPayload(jwtToken);
            String provider = authenticationInfo.get("iss").toString().replace("https://", "");

            credentials = cognitoHelper.GetCredentials(provider, jwtToken);
        }
    }

    public Map<String,String> getAuthenticationInfo() {
        //noinspection unchecked
        return (Map<String,String>)authenticationInfo;
    }

    public String getUserProperty(String propertyName, String defaultValue) {
       return authenticationInfo.getOrDefault(propertyName, defaultValue).toString();
    }

    void resetPassword(String username) {
        cognitoHelper.ResetPassword(username);
    }
    void updatePassword(String username, String password, String pin) {
        cognitoHelper.UpdatePassword(username, password, pin);
    }


    IdentityPersistence getIdentityPersistence() {
        return identityPersistence;
    }

    /**
     * Authenticated means that we have successfully given sign-in credentials to Cognito. We
     * have a token, and can make authenticated server calls.
     * @return True if the user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return credentials != null;
    }

    /**
     * Signid in means that we know who the user is, and they have authenticated at some point,
     * but not necessarily this session.
     * @return True if the user is signed in.
     */
    public boolean isSignedIn() {
        return userEmail != null;
    }

    public boolean isOnline() {
        return AccessControl.isOnline();
    }

    String getAuthMessage() {
        if (authenticationResult == null) return null;
        return authenticationResult.getMessage();
    }

    public String getUserName() {
        return userName;
    }
    public String getuserEmail() {
        return userEmail;
    }

    public TbSrnHelper getTbSrnHelper() {
        if (tbSrnHelper == null) {
            if (userEmail == null) return null;
            tbSrnHelper = new TbSrnHelper(userEmail);
        }
        return tbSrnHelper;
    }

    public ProjectsHelper getProjectsHelper() {
        if (projectsHelper == null) {
            projectsHelper = new ProjectsHelper();
        }
        return projectsHelper;
    }

    public enum SigninResult {FAILURE, SUCCESS, CACHED_OFFLINE, OFFLINE}
    public SigninResult doSignIn(Window parent) {
        Triple<String, String,String> savedSignInDetails = identityPersistence.retrieveSignInDetails();
        SigninResult result;

        if (isOnline()) {
            SigninDialog dialog = new SigninDialog(parent, "Amplio");
            if (savedSignInDetails != null) {
                dialog.setSavedCredentials(savedSignInDetails.getLeft(), savedSignInDetails.getRight());
            }
            dialog.doSignin();

            if (isAuthenticated()) {
                userName = authenticationInfo.get("cognito:username").toString();
                userEmail = authenticationInfo.get("email").toString();
                result = SigninResult.SUCCESS;

                if (dialog.isRememberMeSelected()) {
                    Map<String, String> props = new HashMap<>();
                    authenticationInfo.forEach((k,v)->{props.put(k.toString(),v.toString());});
                    identityPersistence.saveSignInDetails(userName, userEmail, dialog.getPasswordText(), props);
                } else {
                    identityPersistence.clearSignInDetails();
                }

            } else {
                identityPersistence.clearSignInDetails();
                result = SigninResult.FAILURE;
            }
        } else {
            if (savedSignInDetails != null) {
                userName = savedSignInDetails.getLeft();
                userEmail = savedSignInDetails.getMiddle();
                result = SigninResult.CACHED_OFFLINE;
            } else {
                result = SigninResult.OFFLINE;
            }
        }
        return result;
    }

    public void doSignoutAndForgetUser() {
        // Since "signing in" merely obtains credentials for use in future calls, signing out
        // is a matter of forgetting the credentials.
        credentials = null;
        authenticationInfo = null;
        userEmail = null;
        userName = null;
        if (identityPersistence != null) {
            identityPersistence.clearSignInDetails();
        }
    }

    /**
     * Downloads an object from S3, using the credentials of the current signed-in user.
     * @param bucket containing the object
     * @param key of the object
     * @param of output File
     * @param progressHandler optional handler called periodically with (received, expected) bytes.
     */
    public boolean downloadS3Object(String bucket, String key, File of, BiConsumer<Long,Long> progressHandler) {
        long startTime = System.nanoTime();
        AmazonS3 s3Client = getS3Client(credentials);
        boolean done = false;
        long bytesExpected=0, bytesDownloaded=0;

        try (S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucket, key));
            FileOutputStream fos = new FileOutputStream(of);
            BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            InputStream is = s3Object.getObjectContent();
            bytesExpected = s3Object.getObjectMetadata().getContentLength();

            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                bos.write(buffer, 0, bytesRead);
                bytesDownloaded += bytesRead;
                if (progressHandler != null) {
                    progressHandler.accept(bytesDownloaded, bytesExpected);
                }
            }

            done = true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Make a REST call with the current signed-in credentials.
     * @param requestURL URL to request.
     * @return result in a JSON object.
     */
    public JSONObject authenticatedRestCall(String requestURL) {
        if (!isAuthenticated()) return null;
        Map<String,String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "text/plain");
        String idToken = authenticationResult.getIdToken();
        headers.put("Authorization", idToken);

        HttpUtility httpUtility = new HttpUtility();
        JSONObject jsonResponse = null;
        try {
            httpUtility.sendGetRequest(requestURL, headers);
            jsonResponse = httpUtility.readJSONObject();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        httpUtility.disconnect();
        return jsonResponse;
    }
}
