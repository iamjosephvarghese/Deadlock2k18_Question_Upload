package rm.iamjosephvarghese.deadlock2k18_question_upload;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.hash.Hashing;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Upload extends AppCompatActivity {


    private int STORAGE_PERMISSION_CODE = 23;


    private StorageReference mStorageRef;
    private StorageMetadata metadata;



    String answer,downloadUrl;

    private int LOAD_IMAGE = 1;
    private int CHECK_IMAGE = 3;


    Uri uploadUri;
    private Bitmap bitmap;

    String currentHash,previousHash,generatedHash;
    String level;

    Integer levelInt,levelIntNext;

    FirebaseFirestore db;
    DocumentReference documentReference1,documentReference2;


    MaterialDialog.Builder builder,builder1;
    MaterialDialog dialog,uploadDialog;

    Button add;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        add = findViewById(R.id.add);





        db = FirebaseFirestore.getInstance();
        documentReference1 = db.collection("latest").document("updateMe");
        documentReference2 = db.collection("q").document("questions");

        mStorageRef = FirebaseStorage.getInstance().getReference();

        StrictMode.VmPolicy.Builder builderStrict = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builderStrict.build());


        builder = new MaterialDialog.Builder(Upload.this)
                .title("Uploading Image")
                .content("Please Wait")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .cancelable(false);


        uploadDialog = builder.build();


        builder1 = new MaterialDialog.Builder(Upload.this)
                .title("Upload Image")
                .content("Enter Your Choice")
                .positiveText("Select Image")
                .negativeText("Cancel")
                .cancelable(false)
                .alwaysCallInputCallback()
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(1,20,R.color.errorColour)
                .input("Enter Answer", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        if(!input.toString().equals("")){


                            answer = input.toString();

                            Log.d("answer...on progress",input.toString());
                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                        }else if (input.toString().equals("")){
                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        }

                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Log.d("positive clicked","............");


                        metadata = new StorageMetadata.Builder()
                                    .setContentType("image/jpg")
                                    .setCustomMetadata("Timestamp",new Date().toString())
                                    .build();


                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


                            if (isReadStorageAllowed()) {
                                showFileChooser();
                                return;
                            }

                            requestStoragePermission();

                        }else{
                            showFileChooser();
                        }

                    }
                }).onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Log.d("negative clicked","............");

                    }
                });


        dialog = builder1.build();


        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {




                dialog.show();
            }
        });


    }

    private void showFileChooser(){

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Image"),LOAD_IMAGE);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (requestCode == LOAD_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            uploadUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);

                writeToFile(BitMapToString(bitmap),Upload.this);
                Intent showImage = new Intent(Upload.this,ShowSelected.class);
                startActivityForResult(showImage,CHECK_IMAGE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (requestCode == CHECK_IMAGE){
            if (resultCode == RESULT_OK){
                uploadImage();

            }else if(resultCode == RESULT_CANCELED){

            }

        }


    }



    private boolean isReadStorageAllowed() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if (result == PackageManager.PERMISSION_GRANTED)
            return true;

        return false;
    }



    private void requestStoragePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){

        }

        ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == STORAGE_PERMISSION_CODE){

            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){


            }else{


            }
        }


    }

    public void uploadImage(){
        Log.d("Reached","uploadImage");



        db = FirebaseFirestore.getInstance();
        documentReference1 = db.collection("latest").document("updateMe");
        documentReference2 = db.collection("q").document("questions");
        Log.d("Reached","uploadImage2222");


        uploadDialog.show();

        String timestamp = new Date().toString();
        final StorageReference sRef = mStorageRef.child("deadlock_questions/" +  timestamp);

//        ////////////

        documentReference1.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("Reached","ref1listener");
                level = documentSnapshot.get("level").toString();
                Log.d("level",level);
                levelInt = Integer.parseInt(level);
                Log.d("leveInt",levelInt.toString());
                levelIntNext = levelInt + 1;
                Log.d("nextLevel",levelIntNext.toString());

                currentHash = documentSnapshot.get("currentHash").toString();
                previousHash = documentSnapshot.get("previousHash").toString();
                Log.d("currentHash",currentHash);
                Log.d("previousHash",previousHash);



                sRef.putFile(uploadUri,metadata).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        //Toast.makeText(Home.this, "Image Uploaded", Toast.LENGTH_SHORT).show();

                        downloadUrl = taskSnapshot.getDownloadUrl().toString();
                        Log.d("Uploading image",".......");

                        String toBeHashed = answer + downloadUrl + currentHash;


                        Log.d("tobehashed",toBeHashed);

                        generatedHash = Hashing.sha256()
                                .hashString(toBeHashed, Charset.forName("UTF-8"))
                                .toString();



                            Log.d("generatedHash",generatedHash);



                        Map<String,Object> data = new HashMap<>();
                        data.put("photoURL",downloadUrl);



                        Log.d("downloadURL",downloadUrl);


                        WriteBatch batch = db.batch();


                        DocumentReference questionURL = db.collection("q").document("questions").collection(currentHash).document(previousHash);
                        batch.update(questionURL,"photoURL",downloadUrl);



                        Map<String,Object> nullData = new HashMap<>();
                        nullData.put("photoURL",null);
                        nullData.put("level",levelInt + 1);

                        DocumentReference generatedNull = db.collection("q").document("questions").collection(generatedHash).document(currentHash);
                        batch.set(generatedNull,nullData);



                        DocumentReference updatePrevious = db.collection("latest").document("updateMe");
                        batch.update(updatePrevious,"previousHash",currentHash);


                        final DocumentReference updateCurrent = db.collection("latest").document("updateMe");
                        batch.update(updateCurrent,"currentHash",generatedHash);



                        batch.update(updateCurrent,"level",levelInt + 1);



                        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("batch","Push Success");


                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference realRef = database.getReference("answers");


                                realRef.child(levelInt.toString()).setValue(answer).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("realtime","push success");
                                    }
                                });

                                uploadDialog.dismiss();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("batch","Push Error");
                                Log.d("batch",e.toString());
                                uploadDialog.dismiss();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        uploadDialog.dismiss();

                        Log.d("onFaliure",e.toString());
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }



    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }



    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

}
