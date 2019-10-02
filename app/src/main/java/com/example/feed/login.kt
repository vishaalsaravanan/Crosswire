package com.example.feed

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_login.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase




@Suppress("DEPRECATION")
class login : AppCompatActivity() {
    var mAuth: FirebaseAuth? = null
    var database = FirebaseDatabase.getInstance()
    var myRef = database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()
        ivPicture.setOnClickListener( View.OnClickListener
        {
            checkpermission()
        }
        )
    }
    override fun onStart() {

        super.onStart()
        loadfeed()

    }

    fun logintofirebase(email:String,Password:String)
    {
        mAuth!!.createUserWithEmailAndPassword(email, Password)
            .addOnCompleteListener{ task ->
            if(task.isSuccessful) {
                Toast.makeText(applicationContext,"Successful Login",Toast.LENGTH_LONG).show()
                saveimginfirebase()
            } else {
                Toast.makeText(applicationContext,"Login Failed",Toast.LENGTH_LONG).show()
            }

        }
    }
    var downloadurl:String?=null
    var du:String?=null
    var du1:String?=null
    var du3:String="&alt=media"
    fun saveimginfirebase()
    {
        var currentUser= mAuth!!.currentUser
        var email: String? =currentUser!!.email.toString()
        val storage=FirebaseStorage.getInstance()
        var storagRef=storage.getReferenceFromUrl("gs://cresfeed.appspot.com/")
        val df=SimpleDateFormat("ddMMyyHmmss")
        val dataobj=Date()
        val imagepath=SplitString(email!!)+"_"+df.format(dataobj)+".jpg"
        val imageRef=storagRef.child("images/"+imagepath)
        ivPicture.isDrawingCacheEnabled=true
        ivPicture.buildDrawingCache()
        val drawable =ivPicture.drawable as BitmapDrawable
        val bitmap=drawable.bitmap
        val baos=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data=baos.toByteArray()
        val uploadTask=imageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"Failed To Upload",Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { taskSnapshot ->
            imageRef.downloadUrl.addOnCompleteListener (){
                du=taskSnapshot.uploadSessionUri.toString()
                du1=du!!.substring(0,du!!.indexOf("&uploadType"))
                downloadurl=du1+du3
                myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(downloadurl)
                Toast.makeText(applicationContext,""+downloadurl, Toast.LENGTH_LONG).show()
            }
            myRef.child("Users").child(currentUser.uid).child("email").setValue(currentUser.email)
            loadfeed()
        }

    }
    fun SplitString(email:String): String {
        val split=email.split("@")
        return split[0]
    }

    fun loadfeed()
    {
        var currentuser= mAuth!!.currentUser
        if(currentuser!=null)
        {
            var intent =Intent(this,MainActivity::class.java)
            intent.putExtra("email",currentuser.email)
            intent.putExtra("uid",currentuser.uid)
            startActivity(intent)
        }

    }

    val readimage:Int=253
    fun checkpermission()
    {
        if(Build.VERSION.SDK_INT>=23)
        {
            if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission
                    .READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(arrayOf(android.Manifest.permission
                    .READ_EXTERNAL_STORAGE),readimage)
                return
            }
        }
        loadimage()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            readimage->
            {
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    loadimage()
                }
                else
                {
                    Toast.makeText(this,"Cannot access your image",Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    val pic_image=123
    fun loadimage()
    {
        var intent= Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent,pic_image)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode==pic_image&&data!=null&&resultCode== Activity.RESULT_OK)
        {
            val SelectedImage=data.data
            val filepathcol=arrayOf(MediaStore.Images.Media.DATA)
            val cursor=contentResolver.query(SelectedImage!!,filepathcol,null,null,null)
            cursor!!.moveToFirst()
            val colIndex=cursor.getColumnIndex(filepathcol[0])
            val picPath=cursor.getString(colIndex)
            cursor.close()
            ivPicture.setImageBitmap(BitmapFactory.decodeFile(picPath ))

        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    fun bulogin(view: View)
    {
        logintofirebase(etEmail?.text.toString(),etPassword?.text.toString())
    }

}
