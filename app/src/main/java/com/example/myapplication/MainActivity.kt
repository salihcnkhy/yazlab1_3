package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import android.app.Dialog;
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import kotlinx.android.synthetic.main.dialog_layout.*


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    internal lateinit var image: ImageView

    private val IMAGE_PICK_CODE = 1000;
    //Permission code
    private val PERMISSION_CODE_PICK = 1001;
    private val PERMISSION_CODE_TAKE = 1002;
    private val IMAGE_CAPTURE_CODE = 1003
    var image_uri: Uri? = null
    var storage = FirebaseStorage.getInstance()
    var token : String? = ""
    var seekBarValue = 0
    var url = "http://35.198.153.238:5000/predict"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(android.os.Build.VERSION.SDK_INT > 9 ){
           var policy : StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }


        auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword("test@test.com","123456")

        val db = FirebaseFirestore.getInstance()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                seekBarValue = i
                seekbarValue.text = i.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })

        pushCloud.setOnClickListener {

            val uriString = image_uri.toString()
            val uri = Uri.parse(uriString)
            val storageRef = storage.reference
            val imageRef = storageRef.child("image.jpg")
            var uploadTask = imageRef.putFile(uri)
            uploadTask.addOnFailureListener {
                Toast.makeText(this, " denied", Toast.LENGTH_SHORT).show()

            }.addOnSuccessListener {

                val image = hashMapOf(
                    "seg" to isHaveSegmantation.isChecked,
                    "comp" to seekBarValue,
                    "isProcess" to false,
                    "token" to token
                )

                db.collection("image").document("image.jpg").set(image).addOnSuccessListener { documentReference ->
                    // Toast.makeText(this, "Firestore Ok", Toast.LENGTH_SHORT).show()

                    var response = getResponse(url)


                    Toast.makeText(this,response,Toast.LENGTH_LONG).show()
                    showPop(this)

                }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Firestore Not Ok", Toast.LENGTH_SHORT).show()
                    }

            }





        }
        openGalleryButton.setOnClickListener {
            //check runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED){
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE_PICK);
                }
                else{
                    //permission already granted
                    pickImageFromGallery();
                }
            }
            else{
                //system OS is < Marshmallow
                pickImageFromGallery();
            }
        }

        openCameraButton.setOnClickListener {
            //if system os is Marshmallow or Above, we need to request runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                    //permission was not enabled
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_CODE_TAKE)
                }
                else{
                    //permission already granted
                    openCamera()
                }
            }
            else{
                //system os is < marshmallow
                openCamera()
            }
        }




    }
    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE_PICK -> {
                if (grantResults.size >0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }
                else{
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_CODE_TAKE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted
                    openCamera()
                }
                else{
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            imageView.setImageURI(data?.data)
            image_uri = data?.data

        }
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_CAPTURE_CODE){
            //set image captured to image view
            imageView.setImageURI(image_uri)



        }
    }
    fun getResponse(url: String) : String {


            try {
                var client: OkHttpClient = OkHttpClient().newBuilder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()
                var request: Request = Request.Builder()
                    .url(url)
                    .build()

                var response: Response = client.newCall(request).execute();
                return response.body().string()
            } catch (e: Exception) {
                return  e.toString()
            }

        }


    fun showPop(activity: Activity){

        var dialog : Dialog = Dialog(activity)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_layout)
        val storageRef = storage.reference
        val pathReference = storageRef.child("imageProcesed.jpg")

        val ONE_MEGABYTE: Long = 1024 * 1024
        pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener {

            val displayMetrics = DisplayMetrics ()
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            var width = displayMetrics.widthPixels
            var height = displayMetrics.heightPixels
            image = dialog.findViewById(R.id.popImage)
            image.minimumWidth = width -20
            image.minimumHeight = height -20
            val btm : Bitmap = BitmapFactory.decodeByteArray(it,0,it.size)
            image.setImageBitmap(Bitmap.createScaledBitmap(btm, width,
                height, false))
            image.setOnClickListener {
                dialog.dismiss()
            }

        }.addOnFailureListener {
            Toast.makeText(this,"İnmedi",Toast.LENGTH_LONG).show()

        }


        dialog.show()




    }
}

