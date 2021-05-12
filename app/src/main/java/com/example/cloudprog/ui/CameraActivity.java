package com.example.cloudprog.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.example.cloudprog.R;

//lab9-3 import
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends Activity
{
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private ImageView imageView;
    public Bitmap upload_image = null;
    public int unique_num = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        this.imageView = (ImageView)this.findViewById(R.id.image_view);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //CredentialsProvider initialization
        CognitoCachingCredentialsProvider credentialsProvider;
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:6e7847ad-209a-49a8-b46e-4e45d8894fd2", // Use your Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        //s3client initialization
        final AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider.getCredentials());

        //button used to open camera
        Button photoButton = (Button) this.findViewById(R.id.capture_image_btn);
        photoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        //button used to upload picture to aws
        Button uploadButton = (Button) this.findViewById(R.id.s3_btn);
        uploadButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : Upload a picture to S3
                //create a file to write bitmap data
                File f = new File(getCacheDir(), "test1.jpg");
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Convert bitmap to byte array
                Bitmap bitmap = upload_image;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();

                //write the bytes in file && send image to s3-bucket
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();
                    s3Client.putObject(getString(R.string.bucket_name), "upload_image_check_perm.jpg", f);
                    Toast.makeText(CameraActivity.this, "Upload success", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CameraActivity.this, "Upload fail", Toast.LENGTH_LONG).show();
                }
            }
        });

        //button used to send URL of image to sqs
        Button triggerButton = (Button) this.findViewById(R.id.sqs_btn);
        final AmazonSQSClient sqs = new AmazonSQSClient(credentialsProvider.getCredentials());

        triggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // URL: https://sqs.us-east-1.amazonaws.com/099287135517/cloudprog-hw3
                // Url myqueue: https://sqs.us-east-1.amazonaws.com/099287135517/myQueue
                try{
                    //Todo : Get objecturl of the picture from S3
                    String s3objurl = s3Client.getResourceUrl(getString(R.string.bucket_name), "upload_image.jpg");
                    //Todo : Send objecturl to sqs
                    String queue_url = sqs.getQueueUrl(getString(R.string.queue_name)).getQueueUrl();
                    Toast.makeText(CameraActivity.this, queue_url, Toast.LENGTH_LONG).show();

                    SendMessageRequest send_msg_request = new SendMessageRequest()
                            .withQueueUrl(queue_url)
                            .withMessageBody(s3objurl)
                            .withDelaySeconds(5);
                    sqs.sendMessage(send_msg_request);
                    Toast.makeText(CameraActivity.this, "send success", Toast.LENGTH_LONG).show();

                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(CameraActivity.this, "send fail", Toast.LENGTH_LONG).show();
                }

            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            photo = rotateImage(photo, 90);
            imageView.setImageBitmap(photo);
            upload_image = photo;
        }
    }

    //sometime pictures from camera may rotate, this function is used to correct it.
    public static Bitmap rotateImage(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
