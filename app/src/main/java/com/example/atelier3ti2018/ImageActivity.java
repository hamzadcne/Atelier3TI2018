package com.example.atelier3ti2018;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import Models.Comment;

public class ImageActivity extends AppCompatActivity {

    private final static int PICK_IMAGE_REQUEST = 1;
    private final static int CAMERA_REQUEST = 2;
    private final static int READ_EXTARNAL_STORAGE = 99;

    private final static int SOCKET_TIMEOUT_MS = 180000;

    public static final long CAMERA_IMAGE_MAX_DESIRED_SIZE_IN_BYTES = 2524970;
    public static final double CAMERA_IMAGE_MAX_SIZE_AFTER_COMPRESSSION_IN_BYTES = 1893729.0;

    private String stringImage;
    private ImageView imageView;
    private String response;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        imageView = (ImageView) findViewById(R.id.resultImage);
    }

    public void onLoadImageClick(View view) {
        Intent intent = new Intent();

        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
    public void onTakePictureClick(View view){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTARNAL_STORAGE);
            return;
        }
        TakePicture();
    }
    public void TakePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, CAMERA_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == PICK_IMAGE_REQUEST  || requestCode == CAMERA_REQUEST )&& resultCode ==  RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                // Afficher l'image dans un ImageView
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                //imageView.setImageBitmap(bitmap);
                InputStream imageStream = getContentResolver().openInputStream(uri);

                int bitmapSize= sizeOf(bitmap);

                Log.d("img_file_size", "file size in KBs (initially): " + (bitmapSize/1024));

                if(bitmapSize > 500*1024) {
                    bitmap = getResizedBitmap(bitmap, 500,bitmapSize);
                }
                bitmapSize= sizeOf(bitmap);
                Log.d("img_file_size", "file size in KBs (aftercompression): " + (bitmapSize/1024));

                //Convert bitmap to byte array
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                /*ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                byte[] bytearray = bos.toByteArray();
                InputStream in = new ByteArrayInputStream(bos.toByteArray());*/

                // Convertir l'image en binaire
                byte[] imageArray =  InputStreamToByteArray(imageStream);
                byte[] compressedImageArray =  bytes.toByteArray();

                // Convertir du binaire en chaine de caractères
                stringImage = Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT);

                // Envoyer la valeur avec les paramètres de la requêtes POST
                SavePhoto(stringImage);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public byte[] InputStreamToByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        if (stream != null) {
            while ((len = stream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
        }
        return  byteBuffer.toByteArray();
    }
    private void SavePhoto(final String base64photo){
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(
                Request.Method.POST ,
                "http://192.168.43.243:8000/Comment",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
                            Comment result =  gson.fromJson(response,Comment.class);
                            // Après récupération de l'image du Serveur
                            byte[] byteArray = Base64.decode(result.photo,Base64.DEFAULT);
                            Bitmap bitmap1 = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
                            imageView.setImageBitmap(bitmap1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = new String(error.networkResponse.data);
                Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT);
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> params = new HashMap<String, String>();
                params.put("text", "test photo "+ new Date().toString());
                params.put("photo", base64photo);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("token", "user1_token");
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize, long image_size_in_bytes) {
        int width = image.getWidth();
        int height = image.getHeight();


//        if(image_size_in_bytes <= CAMERA_IMAGE_MAX_DESIRED_SIZE_IN_BYTES) {
//            if (width > height) {
//                if (width > 500)
//                    maxSize = width * 75 / 100;
//            } else {
//                if (height > 500)
//                    maxSize = height * 75 / 100;
//            }
//        } else {
//            double percentage = ((CAMERA_IMAGE_MAX_SIZE_AFTER_COMPRESSSION_IN_BYTES/image_size_in_bytes)*100);
//            if (width > height) {
//                if (width > 500)
//                    maxSize = width * (int)percentage / 100;
//            } else {
//                if (height > 500)
//                    maxSize = height * (int)percentage / 100;
//            }
//
//            if(maxSize > 600) {
//                maxSize = 600;
//            }

//        }

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        Bitmap reduced_bitmap = Bitmap.createScaledBitmap(image, width, height, true);
        //        Log.d("file_size","file_size_during_manipulation: "+String.valueOf(sizeOf(reduced_bitmap)));
//        if(sizeOf(reduced_bitmap) > (500 * 1000)) {
//            return getResizedBitmap(reduced_bitmap, maxSize, sizeOf(reduced_bitmap));
//        } else {
            return reduced_bitmap;
//        }
    }
    private int sizeOf(Bitmap bmp){
        return bmp.getByteCount();//bmp.getWidth()*bmp.getHeight();
    }
//    private Bitmap getResizedBitmap(Bitmap bmp,int masSize,long imageSize){
//
//    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_EXTARNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            TakePicture();
    }
}
