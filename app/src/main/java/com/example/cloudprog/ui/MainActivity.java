package com.example.cloudprog.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.AddPermissionRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.example.cloudprog.R;
//import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//lab9-2 import
//delete bucket import
//import com.amazonaws.SdkClientException;
//import com.amazonaws.auth.profile.ProfileCredentialsProvider;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
// sqs import



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:6e7847ad-209a-49a8-b46e-4e45d8894fd2", // Use your Identity pool ID
                Regions.US_EAST_1 // Region
        );
        // Initialize s3Client
        final AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider.getCredentials());
        //init sqs client

        Button button1 = findViewById(R.id.camera_btn);
        button1.setOnClickListener(btn_1_click);

        Button button2 = findViewById(R.id.create_bucket_btn);
        button2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : create a s3 bucket & add permission
                try{
                    //Todo: additional check if bucket exists
                    if (s3Client.doesBucketExist(getString(R.string.bucket_name))){
                        Toast.makeText(MainActivity.this, "Create fail: bucket already exists", Toast.LENGTH_LONG).show();
                    }
                    else{
                        //Check bucket name in app/res/values/string.xml
                        Toast.makeText(MainActivity.this, "Start creating bucket", Toast.LENGTH_LONG).show();
                        AccessControlList acl = new AccessControlList();
                        acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
                        CreateBucketRequest createBucketRequest = new CreateBucketRequest(getString(R.string.bucket_name))
                                .withAccessControlList(acl).withCannedAcl(CannedAccessControlList.PublicReadWrite);
                        s3Client.createBucket(createBucketRequest);
                        Toast.makeText(MainActivity.this, "Create success", Toast.LENGTH_LONG).show();
                        //set policy
                        Policy bucket_policy = new Policy().withStatements(
                                new Statement(Statement.Effect.Allow)
                                        .withPrincipals(Principal.AllUsers)
                                        .withActions(S3Actions.GetObject)
                                        .withResources(new Resource(
                                                "arn:aws:s3:::" + getString(R.string.bucket_name) + "/*")));


//                        String policy = bucket_policy.toJson();

                        try{
                            String policyText = bucket_policy.toString();
                            SetBucketPolicyRequest setBucketPolicyRequest = new SetBucketPolicyRequest(getString(R.string.bucket_name), policyText);
                            s3Client.setBucketPolicy(setBucketPolicyRequest);
//                            s3Client.setBucketPolicy(getString(R.string.bucket_name), bucket_policy.toJson());
                        } catch (AmazonServiceException e) {
                            System.err.println(e.getErrorMessage());
                            Toast.makeText(MainActivity.this, "Error: set policy", Toast.LENGTH_LONG).show();
                        }


                    }
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
                }
//                try{
//                    //Check bucket name in app/res/values/string.xml
//                    AccessControlList acl = new AccessControlList();
//                    acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
//                    CreateBucketRequest createBucketRequest = new CreateBucketRequest(getString(R.string.bucket_name))
//                            .withAccessControlList(acl).withCannedAcl(CannedAccessControlList.PublicReadWrite);
//                    s3Client.createBucket(createBucketRequest);
//                    Toast.makeText(MainActivity.this, "Create success", Toast.LENGTH_LONG).show();
//                }catch(Exception e){
//                    e.printStackTrace();
//                    Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
//                }
            }
        });

        Button button3 = findViewById(R.id.delete_bucket_btn);
        button3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : delete a s3 bucket
                try{
                    String bucketName = getString(R.string.bucket_name);
                    //check if bucket exists
                    if (s3Client.doesBucketExist(getString(R.string.bucket_name))){
                        Toast.makeText(MainActivity.this, "Bucket exists, start deletion process", Toast.LENGTH_LONG).show();
                        //要刪掉bucket要先清空裡面的東西
                        ObjectListing objectListing = s3Client.listObjects(bucketName);
                        while (true) {
                            Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
                            while (objIter.hasNext()) {
                                s3Client.deleteObject(bucketName, objIter.next().getKey());
                            }
                            if (objectListing.isTruncated()) {
                                objectListing = s3Client.listNextBatchOfObjects(objectListing);
                            } else {
                                break;
                            }
                        }
                        //Check bucket name in app/res/values/string.xml
                        DeleteBucketRequest deleteBucketRequest = new DeleteBucketRequest(getString(R.string.bucket_name));
                        s3Client.deleteBucket(deleteBucketRequest);
                        Toast.makeText(MainActivity.this, "Delete success", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Delete fail: bucket not exists", Toast.LENGTH_LONG).show();
                    }

                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Delete fail", Toast.LENGTH_LONG).show();
                }
            }
        });


        Button button4 = findViewById(R.id.create_queue_btn);

        final AmazonSQSClient sqs = new AmazonSQSClient(credentialsProvider.getCredentials());

        button4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {//Todo : create a sqs queue & add permission
                try{
                    String queue_url = sqs.getQueueUrl(getString(R.string.queue_name)).getQueueUrl();
                    //代表 queue exist
                    Toast.makeText(MainActivity.this, "Create fail: queue exists", Toast.LENGTH_LONG).show();

                }catch(Exception e){
                    //代表 queue not exists
                    AccessControlList acl = new AccessControlList();
                    acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
                    CreateQueueRequest create_request = new CreateQueueRequest(getString(R.string.queue_name))
                            .addAttributesEntry("DelaySeconds", "60")
                            .addAttributesEntry("MessageRetentionPeriod", "86400");
                    try {
                        sqs.createQueue(create_request);
                        Toast.makeText(MainActivity.this, "Create success", Toast.LENGTH_LONG).show();
                        String queue_url = sqs.getQueueUrl(getString(R.string.queue_name)).getQueueUrl();

                        Policy policy = new Policy().withStatements(
                                new Statement(Statement.Effect.Allow)
                                    .withPrincipals(Principal.AllUsers)
                                    .withActions(SQSActions.AllSQSActions)
                        );
                        Map queueAttributes = new HashMap();
                        queueAttributes.put(QueueAttributeName.Policy.toString(), policy.toJson());

                        sqs.setQueueAttributes(new SetQueueAttributesRequest(queue_url, queueAttributes));
                    } catch(Exception exc){
                        exc.printStackTrace();
                        Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

        Button button5 = findViewById(R.id.delete_queue_btn);
        button5.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : delete a sqs queue
                try{
                    String queue_url = sqs.getQueueUrl(getString(R.string.queue_name)).getQueueUrl();
                    // 代表有queue
                    Toast.makeText(MainActivity.this, queue_url, Toast.LENGTH_LONG).show();
                    sqs.deleteQueue(queue_url);
                    Toast.makeText(MainActivity.this, "Delete Success", Toast.LENGTH_LONG).show();

                }catch (Exception e){
                    //queue 不存在
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Delete fail: queue not exists", Toast.LENGTH_LONG).show();
                }

            }
        });

    }
    private View.OnClickListener btn_1_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        }
    };
}
