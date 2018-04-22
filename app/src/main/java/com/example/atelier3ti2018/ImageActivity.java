package com.example.atelier3ti2018;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageActivity extends AppCompatActivity {

    private int PICK_IMAGE_REQUEST = 1;
    private String stringImage;
    private ImageView imageView;
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode ==  RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                // Afficher l'image dans un ImageView
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView.setImageBitmap(bitmap);


                InputStream imageStream = getContentResolver().openInputStream(uri);

                // Convertir l'image en binaire
                byte[] imageArray=  InputStreamToByteArray(imageStream);

                // Convertir du binaire en chaine de caractères
                stringImage = Base64.encodeToString(imageArray, Base64.DEFAULT);

                // Envoyer la valeur avec les paramètres de la requêtes POST

                // Après récupération de l'image du Serveur
                byte[] byteArray = Base64.decode(stringImage,Base64.DEFAULT);
                Bitmap bitmap1 = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
                imageView.setImageBitmap(bitmap1);

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
}
