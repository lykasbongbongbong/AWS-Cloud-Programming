package com.example.cloudprog.ui;




import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.AddPermissionRequest;
import com.example.cloudprog.R;

import java.util.Iterator;

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
                "us-east-1:d2931239-99a1-4186-9a39-1d30474f75b5", // Use your Identity pool ID
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
                    //Check bucket name in app/res/values/string.xml
                    AccessControlList acl = new AccessControlList();
                    acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
                    CreateBucketRequest createBucketRequest = new CreateBucketRequest(getString(R.string.bucket_name))
                            .withAccessControlList(acl).withCannedAcl(CannedAccessControlList.PublicReadWrite);
                    s3Client.createBucket(createBucketRequest);
                    Toast.makeText(MainActivity.this, "Create success", Toast.LENGTH_LONG).show();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
                }
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
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Delete fail", Toast.LENGTH_LONG).show();
                }
            }
        });


        Button button4 = findViewById(R.id.create_queue_btn);

        final AmazonSQSClient queueClient = new AmazonSQSClient(credentialsProvider.getCredentials());

        button4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {//Todo : create a sqs queue & add permission
                CreateQueueRequest create_request = new CreateQueueRequest(getString(R.string.queue_name))
                        .addAttributesEntry("DelaySeconds", "60")
                        .addAttributesEntry("MessageRetentionPeriod", "86400");

                try {
                    queueClient.createQueue(create_request);
                } catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
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
