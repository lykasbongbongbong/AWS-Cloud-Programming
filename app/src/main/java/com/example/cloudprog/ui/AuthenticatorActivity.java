package com.example.cloudprog.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.example.cloudprog.R;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import com.facebook.AccessToken;
import com.amazonaws.mobile.auth.facebook.FacebookButton;
import com.amazonaws.mobile.auth.facebook.FacebookSignInProvider;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInActivity;
import com.example.cloudprog.viewmodels.Injection;

import java.util.HashMap;
import java.util.Map;


public class AuthenticatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Todo : Handle cognito signin

        Injection.initialize(getApplicationContext());

        initializeApplication();
        final IdentityManager identityAWSManager = Injection.getAWSService().getIdentityManager();
        final IdentityManager identityFBManager = IdentityManager.getDefaultIdentityManager();


//        FacebookSdk.sdkInitialize(getApplicationContext());
//        AppEventsLogger.activateApp(this);
        // AWS: Set up the callbacks to handle the authentication response
        identityAWSManager.login(this, new DefaultSignInResultHandler() {
                    @Override
                    public void onSuccess(Activity activity, IdentityProvider identityProvider) {
                        Toast.makeText(AuthenticatorActivity.this,
                                String.format("Logged in as %s", identityAWSManager.getCachedUserID()),
                                Toast.LENGTH_LONG).show();
                        // Go to the function activity
                        final Intent intent = new Intent(activity, MainActivity.class) //MainActivity
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(intent);
                        activity.finish();
                    }

                    @Override
                    public boolean onCancel(Activity activity) {
                        return false;
                    }
                }
        );

        // FB: Set up the callbacks to handle the authentication response
        identityFBManager.login(this, new DefaultSignInResultHandler() {
                    @Override
                    public void onSuccess(Activity activity, IdentityProvider identityProvider) {
                        Map<String, String> logins = new HashMap<String, String>();
                        logins.put("graph.facebook.com", AccessToken.getCurrentAccessToken().getToken());
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();
                        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();

                        Toast.makeText(AuthenticatorActivity.this,
                                String.format("Signed in with %s",
                                        identityProvider.getDisplayName()), Toast.LENGTH_LONG).show();
                        // Go to the function activity
                        final Intent intent = new Intent(activity, MainActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(intent);
                        activity.finish();
                    }

                    @Override
                    public boolean onCancel(Activity activity) {
                        return false;
                    }
                }
        );

        // Start the authentication UI
        AuthUIConfiguration config = new AuthUIConfiguration.Builder()
                .userPools(true)
                .signInButton(FacebookButton.class) // Show Facebook button
                .build();
        SignInActivity.startSignInActivity(this, config);
        AuthenticatorActivity.this.finish();
    }


    private void initializeApplication() {
        AWSConfiguration awsConfiguration = new AWSConfiguration(getApplicationContext());

        // If IdentityManager is not created, create it
        if(IdentityManager.getDefaultIdentityManager() == null){
            IdentityManager identityFBManager =
                    new IdentityManager(getApplicationContext(),awsConfiguration);
            IdentityManager.setDefaultIdentityManager(identityFBManager);
        }

        // Add Facebook as Identity Provider
        IdentityManager.getDefaultIdentityManager().addSignInProvider(
                FacebookSignInProvider.class);

        FacebookSignInProvider.setPermissions("public_profile");

    }
}