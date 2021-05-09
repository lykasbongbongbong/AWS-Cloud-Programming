package com.example.cloudprog.ui;

//9-1 import start
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInActivity;
import com.example.cloudprog.viewmodels.Injection;
//end

//lab9-2 import
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import android.os.StrictMode;
//end

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cloudprog.R;


public class AuthenticatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);
        //Todo : Handle cognito signin
        //testing branch
        Injection.initialize(getApplicationContext());

        final IdentityManager identityManager = Injection.getAWSService().getIdentityManager();
        // Set up the callbacks to handle the authentication response
        identityManager.login(this, new DefaultSignInResultHandler() {
            @Override
            public void onSuccess(Activity activity, IdentityProvider identityProvider) {
                Toast.makeText(AuthenticatorActivity.this,
                        String.format("Logged in as %s", identityManager.getCachedUserID()),
                        Toast.LENGTH_LONG).show();
                // Go to the main activity
                final Intent intent = new Intent(activity, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                activity.finish();
            }

            @Override
            public boolean onCancel(Activity activity) {
                return false;
            }
        });

        // Start the authentication UI
        AuthUIConfiguration config = new AuthUIConfiguration.Builder()
                .userPools(true)
                .build();
        SignInActivity.startSignInActivity(this, config);
        AuthenticatorActivity.this.finish();
    }
}
