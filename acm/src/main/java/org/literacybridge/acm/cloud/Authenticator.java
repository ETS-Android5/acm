package org.literacybridge.acm.cloud;

import com.amazonaws.services.cognitoidentity.model.Credentials;
import org.json.simple.JSONObject;
import org.literacybridge.acm.cloud.cognito.AuthenticationHelper;
import org.literacybridge.acm.cloud.cognito.CognitoHelper;
import org.literacybridge.acm.cloud.cognito.CognitoJWTParser;
import org.literacybridge.acm.config.HttpUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Helper class to provide a simpler interface to cognito authentication.
 */
public class Authenticator {
    private static Authenticator instance;

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

    private Authenticator() {
        cognitoHelper = new CognitoHelper();
    }

    public void authenticate(String username, String password) {

        long startTime = System.nanoTime();
        authenticationResult = cognitoHelper.ValidateUser(username, password);
        String jwtToken = authenticationResult.getJwtToken();
        long validatedTime = System.nanoTime();
        if (jwtToken != null) {
            System.out.println(jwtToken);
            System.out.printf("User is authenticated in %d ms.\n",
                (validatedTime - startTime) / 1000000);
        } else {
            System.out.println("Username/password is invalid.");
            return;
        }

        authenticationInfo = CognitoJWTParser.getPayload(jwtToken);
        String provider = authenticationInfo.get("iss").toString().replace("https://", "");

        credentials = cognitoHelper.GetCredentials(provider, jwtToken);

    }

    public void resetPassword(String username) {
        cognitoHelper.ResetPassword(username);
    }
    public void updatePassword(String username, String password, String pin) {
        cognitoHelper.UpdatePassword(username, password, pin);
    }

    public Collection<String> getProjects() {
        List<String> result = new ArrayList<>();
        // Statistics, configured in AWS Application Gateway
        String baseURL = "https://y06knefb5j.execute-api.us-west-2.amazonaws.com/Devo";
        String requestURL = baseURL + "/projects";
        JSONObject request = new JSONObject();
        Map<String,String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Authentication", authenticationResult.getIdToken());

        HttpUtility httpUtility = new HttpUtility();
        JSONObject jsonResponse = null;
        try {
            httpUtility.sendGetRequest(requestURL, headers);
            jsonResponse = httpUtility.readJSONObject();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        httpUtility.disconnect();

        return result;
    }

    @SuppressWarnings("unchecked")
    public boolean canViewProject(String project) {
        if (!isAuthenticated()) return false;
        String viewParam = (String)authenticationInfo.getOrDefault("view", "");
        String editParam = (String)authenticationInfo.getOrDefault("edit", "");
        Pattern viewPattern = Pattern.compile(viewParam, CASE_INSENSITIVE);
        Pattern editPattern = Pattern.compile(editParam, CASE_INSENSITIVE);

        return viewPattern.matcher(project).matches() || editPattern.matcher(project).matches();
    }

    public boolean isAuthenticated() {
        return credentials != null;
    }

    public String getAuthMessage() {
        if (authenticationResult == null) return null;
        return authenticationResult.getMessage();
    }
}
