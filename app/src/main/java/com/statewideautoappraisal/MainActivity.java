package com.statewideautoappraisal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_GALLERY = 1;
    private static final int REQUEST_CAMERA = 2;
    EditText edtName, edtPhone, edtEmail, edtComments, edtYear, edtMake, edtModel;
    TextView txvImageGuideline, txvRightFrontUploadImage, txvLeftFrontUploadImage,
            txvRightRearUploadImage, txvLeftRearUploadImage, txvLicensePlate, txvVinNumber,
            txvMileage, txvInterior, txvDamage1, txvDamage2, txvDamage3;
    ImageView imvRightFrontImage, imvLeftFrontImage, imvRightRearImage, imvLeftRearImage,
            imvLicensePlateImage, imvVinNumberImage, imvMileageImage, imvInteriorImage, imvDamageImage1,
            imvDamageImage2, imvDamageImage3;
    Button btnSubmit;
    String imagecall = "", getRightFrontImage = "", getLeftFrontImage = "", getRightRearImage = "", getLeftRearImage = "",
            getLicensePlateImage = "", getVinNumberImage = "", getMileageImage = "", getInteriorImage = "", getDamageImage1 = "",
            getDamageImage2 = "", getDamageImage3 = "";
    private Uri mFileUri;

    public static void createDirectory(String filePath) {
        if (!new File(filePath).exists()) {
            new File(filePath).mkdirs();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPathFromGallery(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (!isKitKat) {
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = context.getContentResolver().query(uri,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        } else if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }
                // handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        edtName = (EditText) findViewById(R.id.edtName);
        edtPhone = (EditText) findViewById(R.id.edtPhone);
        edtEmail = (EditText) findViewById(R.id.edtEmail);
        edtComments = (EditText) findViewById(R.id.edtComments);
        edtYear = (EditText) findViewById(R.id.edtYear);
        edtMake = (EditText) findViewById(R.id.edtMake);
        edtModel = (EditText) findViewById(R.id.edtModel);

        txvImageGuideline = (TextView) findViewById(R.id.txvImageGuideline);
        txvRightFrontUploadImage = (TextView) findViewById(R.id.txvRightFrontUploadImage);
        txvLeftFrontUploadImage = (TextView) findViewById(R.id.txvLeftFrontUploadImage);
        txvRightRearUploadImage = (TextView) findViewById(R.id.txvRightRearUploadImage);
        txvLeftRearUploadImage = (TextView) findViewById(R.id.txvLeftRearUploadImage);
        txvLicensePlate = (TextView) findViewById(R.id.txvLicensePlate);
        txvVinNumber = (TextView) findViewById(R.id.txvVinNumber);
        txvMileage = (TextView) findViewById(R.id.txvMileage);
        txvInterior = (TextView) findViewById(R.id.txvInterior);
        txvDamage1 = (TextView) findViewById(R.id.txvDamage1);
        txvDamage2 = (TextView) findViewById(R.id.txvDamage2);
        txvDamage3 = (TextView) findViewById(R.id.txvDamage3);

        imvRightFrontImage = (ImageView) findViewById(R.id.imvRightFrontImage);
        imvLeftFrontImage = (ImageView) findViewById(R.id.imvLeftFrontImage);
        imvRightRearImage = (ImageView) findViewById(R.id.imvRightRearImage);
        imvLeftRearImage = (ImageView) findViewById(R.id.imvLeftRearImage);
        imvLicensePlateImage = (ImageView) findViewById(R.id.imvLicensePlateImage);
        imvVinNumberImage = (ImageView) findViewById(R.id.imvVinNumberImage);
        imvMileageImage = (ImageView) findViewById(R.id.imvMileageImage);
        imvInteriorImage = (ImageView) findViewById(R.id.imvInteriorImage);
        imvDamageImage1 = (ImageView) findViewById(R.id.imvDamageImage1);
        imvDamageImage2 = (ImageView) findViewById(R.id.imvDamageImage2);
        imvDamageImage3 = (ImageView) findViewById(R.id.imvDamageImage3);

        btnSubmit = (Button) findViewById(R.id.btnSubmit);

        txvImageGuideline.setOnClickListener(this);
        txvRightFrontUploadImage.setOnClickListener(this);
        txvLeftFrontUploadImage.setOnClickListener(this);
        txvRightRearUploadImage.setOnClickListener(this);
        txvLeftRearUploadImage.setOnClickListener(this);
        txvLicensePlate.setOnClickListener(this);
        txvVinNumber.setOnClickListener(this);
        txvMileage.setOnClickListener(this);
        txvInterior.setOnClickListener(this);
        txvDamage1.setOnClickListener(this);
        txvDamage2.setOnClickListener(this);
        txvDamage3.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
    }

    public void imageCall() {
        final CharSequence[] options = {"Take Photo",
                "Choose from Gallery", "Cancel"};

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("Take Photo")) {
                            startCamera();
                        } else if (options[item]
                                .equals("Choose from Gallery")) {
                            startGallery();
                        } else if (options[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
        builder.show();
    }

    private void startCamera() {
        Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mFileUri = getOutputMediaFile(1);
        if (mFileUri != null) {
            intent1.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
            startActivityForResult(intent1, REQUEST_CAMERA);
        } else {
            Log.e("image camera", "file not available");
        }
    }

    private void startGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY);
    }

    public Uri getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "HomeInspection");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("survey form second", "could not create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File mediaFile;
        if (type == 1) {
            String imageStoragePath = mediaStorageDir + "/Images/";
            createDirectory(imageStoragePath);
            mediaFile = new File(imageStoragePath + "IMG" + timeStamp + ".jpg");

        } else {
            return null;
        }
        return Uri.fromFile(mediaFile);
    }

    private Bitmap getThumbnailBitmap(final String path, final int thumbnailSize) {
        Bitmap bitmap;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
            bitmap = null;
        }
        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
                : bounds.outWidth;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / thumbnailSize;
        bitmap = BitmapFactory.decodeFile(path, opts);
        return bitmap;
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public String getPath(Uri uri, boolean isImage) {
        if (uri == null) {
            return null;
        }
        String[] projection;
        String coloumnName, selection;
        if (isImage) {
            selection = MediaStore.Images.Media._ID + "=?";
            coloumnName = MediaStore.Images.Media.DATA;
        } else {
            selection = MediaStore.Video.Media._ID + "=?";
            coloumnName = MediaStore.Video.Media.DATA;
        }
        projection = new String[]{coloumnName};
        Cursor cursor;
        if (Build.VERSION.SDK_INT > 19) {
            // Will return "image:x*"
            String wholeID = DocumentsContract.getDocumentId(uri);
            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];
            // where id is equal to
            if (isImage) {
                cursor = getContentResolver()
                        .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection,
                                new String[]{id}, null);
            } else {
                cursor = getContentResolver()
                        .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, new String[]{id},
                                null);
            }
        } else {
            cursor = getContentResolver().query(uri, projection, null, null, null);
        }
        String path = null;
        try {
            int column_index = cursor.getColumnIndex(coloumnName);
            cursor.moveToFirst();
            path = cursor.getString(column_index);
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_CAMERA) {
                String selectedImage = "";
                if (mFileUri != null) {
                    Log.d("upload image", "file: " + mFileUri);
                    selectedImage = getRealPathFromURI(mFileUri);
                } else {
                    if (data != null) {
                        try {
                            selectedImage = getPath(data.getData(), true);
                        } catch (Exception e) {
                            selectedImage = getRealPathFromURI(data.getData());
                        }
                    }
                }
                if (imagecall.equalsIgnoreCase("RIGHT_FRONT")) {
                    getRightFrontImage = selectedImage;
                    imvRightFrontImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("LEFT_FRONT")) {
                    getLeftFrontImage = selectedImage;
                    imvLeftFrontImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("RIGHT_REAR")) {
                    getRightRearImage = selectedImage;
                    imvRightRearImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("LEFT_REAR")) {
                    getLeftRearImage = selectedImage;
                    imvLeftRearImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("LICENSE")) {
                    getLicensePlateImage = selectedImage;
                    imvLicensePlateImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("VIN_NUMBER")) {
                    getVinNumberImage = selectedImage;
                    imvVinNumberImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("MILEAGE")) {
                    getMileageImage = selectedImage;
                    imvMileageImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("INTERIOR")) {
                    getInteriorImage = selectedImage;
                    imvInteriorImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("DAMAGE1")) {
                    getDamageImage1 = selectedImage;
                    imvDamageImage1.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("DAMAGE2")) {
                    getDamageImage2 = selectedImage;
                    imvDamageImage2.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                } else if (imagecall.equalsIgnoreCase("DAMAGE3")) {
                    getDamageImage3 = selectedImage;
                    imvDamageImage3.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                }

                // ivTakePhoto.setImageBitmap(getBitmap(new File(selectedImage)));
            } else if (requestCode == REQUEST_GALLERY) {
                String selectedImage = "";
                Uri selectedImageUri = data.getData();
                Log.e("Image path: ", selectedImageUri.toString());
                selectedImage = getPathFromGallery(MainActivity.this, selectedImageUri);
                Log.e("Image path:", selectedImage);
                System.out.println("Image Path : " + selectedImage);

//                Bitmap bitmap = BitmapFactory.decodeFile(selectedImage);
//                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
                // Bitmap conv_bm = getRoundedRectBitmap(resized, 200);
                if (data != null && data.getData() != null) {
                    if (imagecall.equalsIgnoreCase("RIGHT_FRONT")) {
                        getRightFrontImage = selectedImage;
                        imvRightFrontImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("LEFT_FRONT")) {
                        getLeftFrontImage = selectedImage;
                        imvLeftFrontImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("RIGHT_REAR")) {
                        getRightRearImage = selectedImage;
                        imvRightRearImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("LEFT_REAR")) {
                        getLeftRearImage = selectedImage;
                        imvLeftRearImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("LICENSE")) {
                        getLicensePlateImage = selectedImage;
                        imvLicensePlateImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("VIN_NUMBER")) {
                        getVinNumberImage = selectedImage;
                        imvVinNumberImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("MILEAGE")) {
                        getMileageImage = selectedImage;
                        imvMileageImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("INTERIOR")) {
                        getInteriorImage = selectedImage;
                        imvInteriorImage.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("DAMAGE1")) {
                        getDamageImage1 = selectedImage;
                        imvDamageImage1.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("DAMAGE2")) {
                        getDamageImage2 = selectedImage;
                        imvDamageImage2.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    } else if (imagecall.equalsIgnoreCase("DAMAGE3")) {
                        getDamageImage3 = selectedImage;
                        imvDamageImage3.setImageBitmap(getThumbnailBitmap(selectedImage, 200));
                    }
                }

            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txvImageGuideline:
                Intent i = new Intent(MainActivity.this, ImageOpenActivity.class);
                startActivity(i);
                break;
            case R.id.txvRightFrontUploadImage:
                imagecall = "RIGHT_FRONT";
                imageCall();
                break;
            case R.id.txvLeftFrontUploadImage:
                imagecall = "LEFT_FRONT";
                imageCall();
                break;
            case R.id.txvRightRearUploadImage:
                imagecall = "RIGHT_REAR";
                imageCall();
                break;
            case R.id.txvLeftRearUploadImage:
                imagecall = "LEFT_REAR";
                imageCall();
                break;
            case R.id.txvLicensePlate:
                imagecall = "LICENSE";
                imageCall();
                break;
            case R.id.txvVinNumber:
                imagecall = "VIN_NUMBER";
                imageCall();
                break;
            case R.id.txvMileage:
                imagecall = "MILEAGE";
                imageCall();
                break;
            case R.id.txvInterior:
                imagecall = "INTERIOR";
                imageCall();
                break;
            case R.id.txvDamage1:
                imagecall = "DAMAGE1";
                imageCall();
                break;
            case R.id.txvDamage2:
                imagecall = "DAMAGE2";
                imageCall();
                break;
            case R.id.txvDamage3:
                imagecall = "DAMAGE3";
                imageCall();
                break;
            case R.id.btnSubmit:
//                getRightFrontImage = "", getLeftFrontImage = "", getRightRearImage = "", getLeftRearImage = "",
//                        getLicensePlateImage = "", getVinNumberImage = "", getMileageImage = "", getInteriorImage = "", getDamageImage1 = "",
//                        getDamageImage2 = "", getDamageImage3 = ""
                //edtName, edtPhone, edtEmail, edtComments, edtYear, edtMake, edtModel
                if (edtName.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter Name", Toast.LENGTH_LONG).show();
                } else if (edtPhone.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter Phone Number", Toast.LENGTH_LONG).show();
                } else if (edtEmail.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter Email", Toast.LENGTH_LONG).show();
                } else if (!isValidEmail(edtEmail.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please enter valid Email", Toast.LENGTH_LONG).show();
                } else if (edtComments.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter comments", Toast.LENGTH_LONG).show();
                } else if (edtYear.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter year", Toast.LENGTH_LONG).show();
                } else if (edtMake.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter Make value", Toast.LENGTH_LONG).show();
                } else if (edtModel.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter Model", Toast.LENGTH_LONG).show();
                } else if (getRightFrontImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Right front image", Toast.LENGTH_LONG).show();
                } else if (getLeftFrontImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Left front image", Toast.LENGTH_LONG).show();
                } else if (getRightRearImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Right rear image", Toast.LENGTH_LONG).show();
                } else if (getLeftRearImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Left rear image", Toast.LENGTH_LONG).show();
                } else if (getLicensePlateImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose License plate image", Toast.LENGTH_LONG).show();
                } else if (getVinNumberImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Vin number image", Toast.LENGTH_LONG).show();
                } else if (getMileageImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Mileage image", Toast.LENGTH_LONG).show();
                } else if (getInteriorImage.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please choose Interior image", Toast.LENGTH_LONG).show();
                } else {
                    final String name = edtName.getText().toString();
                    final String phone = edtPhone.getText().toString();
                    final String email = edtEmail.getText().toString();
                    final String comment = edtComments.getText().toString();
                    final String year = edtYear.getText().toString();
                    final String make = edtMake.getText().toString();
                    final String model = edtModel.getText().toString();

                    String Body = "Name: " + name + "\n" +
                            "Phone: " + phone + "\n" +
                            "Email: " + email + "\n" +
                            "Comments: " + comment + "\n" +
                            "Year: " + year + "\n" +
                            "Make: " + make + "\n" +
                            "Model: " + model;
                    new AsyncTask<Void, Void, String>() {
                        ProgressDialog mProgressDialog;

                        @Override
                        protected String doInBackground(Void... params) {
                            // TODO Auto-generated method stub
                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = new HttpPost("http://axtondemos.com/demo1/sendmailapi.php");

                            try {

                                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

                                entity.addPart("name", new StringBody(name));
                                entity.addPart("phone", new StringBody(phone));
                                entity.addPart("email", new StringBody(email));
                                entity.addPart("comments", new StringBody(comment));
                                entity.addPart("year", new StringBody(year));
                                entity.addPart("make", new StringBody(make));
                                entity.addPart("model", new StringBody(model));
                                entity.addPart("file1", new FileBody(new File(compressImage(getRightFrontImage))));
                                entity.addPart("file2", new FileBody(new File(compressImage(getLeftFrontImage))));
                                entity.addPart("file3", new FileBody(new File(compressImage(getRightRearImage))));
                                entity.addPart("file4", new FileBody(new File(compressImage(getLeftRearImage))));
                                entity.addPart("file5", new FileBody(new File(compressImage(getLicensePlateImage))));
                                entity.addPart("file6", new FileBody(new File(compressImage(getVinNumberImage))));
                                entity.addPart("file7", new FileBody(new File(compressImage(getMileageImage))));
                                entity.addPart("file8", new FileBody(new File(compressImage(getInteriorImage))));
                                if (!getDamageImage1.isEmpty()) {
                                    entity.addPart("file9", new FileBody(new File(compressImage(getDamageImage1))));
                                }
                                if (!getDamageImage2.isEmpty()) {
                                    entity.addPart("file10", new FileBody(new File(compressImage(getDamageImage2))));
                                }
                                if (!getDamageImage3.isEmpty()) {
                                    entity.addPart("file11", new FileBody(new File(compressImage(getDamageImage3))));
                                }
                                httppost.setEntity(entity);
                                HttpResponse response = httpclient.execute(httppost);
                                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(response.getEntity()
                                                .getContent()));
                                StringBuffer sb = new StringBuffer("");
                                String line = "";
                                while ((line = in.readLine()) != null) {
                                    sb.append(line);
                                }
                                in.close();

                                return sb.toString();

                            } catch (Exception e) {
                                Log.e("send mail error", "" + e);
                                return "";
                            }

                        }

                        @Override
                        protected void onPostExecute(String result) {
                            // TODO Auto-generated method stub
                            super.onPostExecute(result);
                            Log.e("send mail", result.toString());
                            try {
                                mProgressDialog.dismiss();
                                JSONObject object = new JSONObject(result.toString());
                                if (object.getString("result").equalsIgnoreCase("true")) {
                                    Toast.makeText(MainActivity.this, "Thank you! Your report submitted successfully", Toast.LENGTH_LONG).show();
                                    edtName.setText("");
                                    edtPhone.setText("");
                                    edtEmail.setText("");
                                    edtComments.setText("");
                                    edtYear.setText("");
                                    edtMake.setText("");
                                    edtModel.setText("");
                                    getRightFrontImage = "";
                                    getLeftFrontImage = "";
                                    getRightRearImage = "";
                                    getLeftRearImage = "";
                                    getLicensePlateImage = "";
                                    getVinNumberImage = "";
                                    getMileageImage = "";
                                    getInteriorImage = "";
                                    getDamageImage1 = "";
                                    getDamageImage2 = "";
                                    getDamageImage3 = "";
                                    imvRightFrontImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvLeftFrontImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvRightRearImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvLeftRearImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvLicensePlateImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvVinNumberImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvMileageImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvInteriorImage.setImageResource(R.drawable.ic_square_no_image);
                                    imvDamageImage1.setImageResource(R.drawable.ic_square_no_image);
                                    imvDamageImage2.setImageResource(R.drawable.ic_square_no_image);
                                    imvDamageImage3.setImageResource(R.drawable.ic_square_no_image);
                                } else {
                                    Toast.makeText(MainActivity.this, "Oops! we can't send your report \nPlease try again", Toast.LENGTH_LONG).show();
                                }

                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                if (e instanceof TimeoutError || e instanceof NoConnectionError) {
                                    Toast.makeText(MainActivity.this, "Please check your internet connection!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Oops! we can't send your report \nPlease try again", Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                        @Override
                        protected void onPreExecute() {
                            // TODO Auto-generated method stub
                            super.onPreExecute();
                            mProgressDialog = new ProgressDialog(MainActivity.this);
                            mProgressDialog.setTitle("");
                            mProgressDialog.setCanceledOnTouchOutside(false);
                            mProgressDialog.setMessage("Please Wait...");
                            mProgressDialog.show();

                        }
                    }.execute();
/*
                    try {
                        GMailSender sender = new GMailSender(this, "", "");
                        sender.addAttachment(compressImage(getRightFrontImage), "Right Front Image");
                        sender.addAttachment(compressImage(getLeftFrontImage), "Left Front Image");
                        sender.addAttachment(compressImage(getRightRearImage), "Right Rear Image");
                        sender.addAttachment(compressImage(getLeftRearImage), "Left Rear Image");
                        sender.addAttachment(compressImage(getLicensePlateImage), "License plate Image");
                        sender.addAttachment(compressImage(getVinNumberImage), "Vin number Image");
                        sender.addAttachment(compressImage(getMileageImage), "Mileage Image");
                        sender.addAttachment(compressImage(getInteriorImage), "Interior Image");
                        if (!getDamageImage1.isEmpty()) {
                            sender.addAttachment(compressImage(getDamageImage1), "Damage1 Image");
                        }
                        if (!getDamageImage2.isEmpty()) {
                            sender.addAttachment(compressImage(getDamageImage2), "Damage2 Image");
                        }
                        if (!getDamageImage3.isEmpty()) {
                            sender.addAttachment(compressImage(getDamageImage3), "Damage3 Image");
                        }
                        boolean status = sender.sendMail("Statewide Auto Appraisal report",
                                Body,
                                "demo1.testing1@gmail.com", email);

                    } catch (Exception e) {
                        Log.e("SendMail", e.getMessage());
                       // String log = e.getMessage();
                        try {
                            Writer writer = new StringWriter();
                            PrintWriter printWriter = new PrintWriter(writer);
                            e.printStackTrace(printWriter);
                            String s = writer.toString();

                            File myFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AUTOAPPRIASAL/Log1.txt");
                            myFile.createNewFile();
                            FileOutputStream fOut = new FileOutputStream(myFile);
                            OutputStreamWriter myOutWriter =
                                    new OutputStreamWriter(fOut);
                            myOutWriter.append("printstacktrace:"+s+"\nmessage:" + e.getMessage()+"\ncause:"+e.getCause()+"\nstacktrace:"+e.getStackTrace()+"localized message:"+e.getLocalizedMessage());
                            myOutWriter.close();
                            fOut.close();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            Toast.makeText(MainActivity.this, e1 + "Oops! we can't send your report \nPlease try again", Toast.LENGTH_LONG).show();
                        }
                    }
*/

                }
                break;
        }
    }

    private Bitmap getThumbnailBitmap(final String path) {
        Bitmap bitmap;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
            bitmap = null;
        }
        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
                : bounds.outWidth;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / 600;
        bitmap = BitmapFactory.decodeFile(path, opts);
        return bitmap;
    }

    public String compressImage(String path) {
        Bitmap original = getThumbnailBitmap(path);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, 90, out);
        //   Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));

        try {
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/AUTOAPPRIASAL/";
            String filename = path.substring(path.lastIndexOf("/") + 1, path.length());
            File file = new File(extStorageDirectory);
            if (file.exists()) {
                file.delete();
                file = new File(extStorageDirectory);
                Log.e("file exist", "" + file + ",Bitmap= " + filename);
            } else {
                if (!file.mkdirs()) {
                    Log.e("External storage not ", "available");
                }
            }
            File mediafile = new File(file.getPath() + File.separator + filename);
            FileOutputStream fOut = new FileOutputStream(mediafile);
            fOut.write(out.toByteArray());
            //bitmap.compress(null,100, fOut);
            fOut.flush();
            fOut.close();
            Log.e("path", ":" + file.getPath() + File.separator + filename);
            return file.getPath() + File.separator + filename;
        } catch (Exception e) {
            e.printStackTrace();
            return path;
        }
    }

   /* private File savebitmap(String filename) {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;

        File file = new File(filename + ".png");
        if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, filename + ".png");
            Log.e("file exist", "" + file + ",Bitmap= " + filename);
        }
        try {
            // make a new bitmap from your file
            Bitmap bitmap = BitmapFactory.decodeFile(file.getName());

            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("file", "" + file);
        return file;

    }*/

    public boolean isValidEmail(String email) {
        boolean isValidEmail = false;
        System.out.println(email);
        String emailExpression = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(emailExpression,
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValidEmail = true;
        }
        return isValidEmail;
    }

}
