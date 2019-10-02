package com.example.feed

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.addticket.view.*
import kotlinx.android.synthetic.main.feedactivity.*
import kotlinx.android.synthetic.main.feedactivity.view.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    var tweetlist = ArrayList<ticket>()
    var adapter:feedadapter?=null
    var myemail:String?=null
    var database = FirebaseDatabase.getInstance()
    var myRef = database.reference
    var userId:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var b:Bundle?=intent.extras
        if (b != null) {
            userId=b.getString("uid")
            myemail=b.getString("email")
        }
        tweetlist.add(ticket("0", "New Post", "url", "add"))
        adapter=feedadapter(this,tweetlist)
        lvfeed.adapter=adapter
        loadpost()
    }

    inner class feedadapter : BaseAdapter {
        var feedlist: ArrayList<ticket>
        var context: Context? = null

        constructor(context: Context, feedlist: ArrayList<ticket>) : super() {
            this.feedlist = feedlist
            this.context = context
        }

        override fun getView(index: Int, p1: View?, p2: ViewGroup?): View {
            val feed = feedlist[index]

            if (feed.postpersonid.equals("add")) {
                var myview = layoutInflater.inflate(R.layout.addticket, null)
                myview.ivAttach.setOnClickListener(View.OnClickListener {
                    loadimage()
                })
                myview.ivPost.setOnClickListener(View.OnClickListener {
                    myRef.child("posts").push().setValue(postInfo(userId!!,myview.etText.text.toString(),downloadurl.toString()))
                   myview.etText.setText("")

                    })
                return myview
            }else if(feed.postpersonid.equals("loading"))
            {
                var myview = layoutInflater.inflate(R.layout.loadinglayout, null)
                return  myview

            }
            else if(feed.postpersonid.equals("rev")) {
                var myview = layoutInflater.inflate(R.layout.rev_ticket, null)
                var mAdView = myview.findViewById(R.id.adView) as AdView
                val adRequest = AdRequest.Builder().build()
                mAdView.loadAd(adRequest)
                return myview
            }
                else {
                var myview = layoutInflater.inflate(R.layout.feedactivity, null)
                myview.feedtext.setText(feed.posttext)
                Picasso.get().load(feed.postimgurl).into(myview.feedimage)
                myRef.child("Users").child(feed.postpersonid!!)
                    .addValueEventListener(object :ValueEventListener{
                        override fun onCancelled(p0: DatabaseError) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onDataChange(DataSnapshot: DataSnapshot) {
                            try{
                                var td=DataSnapshot!!.value as HashMap <String,Any>
                                for (key in td.keys)
                                {
                                    var userInfo=td[key] as String
                                    if(key.equals("ProfileImage"))
                                    {
                                        Picasso.get().load(userInfo).into(myview.profilepic)
                                    }
                                    else
                                    {
                                        myview.usernametxt.setText(SplitString(userInfo))

                                    }
                                }

                            }catch (ex:Exception){}
                        }

                    })

                return myview

            }
        }
        override fun getItem(index: Int): Any {
            return feedlist[index]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            return feedlist.size
        }


    }
    val pic_image=123
    fun loadimage()
    {
        var intent= Intent(
            Intent.ACTION_PICK,
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
            if((BitmapFactory.decodeFile(picPath))!=null) {
                UploadImage(BitmapFactory.decodeFile(picPath))
            }
            }
        super.onActivityResult(requestCode, resultCode, data)
    }
    var downloadurl:String?=null
    var du:String?=null
    var du1:String?=null
    var du3:String="&alt=media"
    @SuppressLint("SimpleDateFormat")
    fun UploadImage(bitmap: Bitmap)
    {
        tweetlist.add(0,ticket("0", "New Post", "url", "loading"))
        adapter!!.notifyDataSetChanged()
        val storage= FirebaseStorage.getInstance()
            var storagRef=storage.getReferenceFromUrl("gs://cresfeed.appspot.com/")
            val df= SimpleDateFormat("ddMMyyHHmmss")
            val dataobj= Date()
            val imagepath=SplitString(myemail!!)+"_"+df.format(dataobj)+".jpg"
            val imageRef=storagRef.child("imagesPost/"+imagepath)
            val baos= ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
            val data=baos.toByteArray()
            val uploadTask=imageRef.putBytes(data)
            uploadTask.addOnFailureListener{
                Toast.makeText(applicationContext,"Failed To Upload", Toast.LENGTH_LONG).show()
            }.addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnCompleteListener (){
                    du=taskSnapshot.uploadSessionUri.toString()
                    du1=du!!.substring(0,du!!.indexOf("&uploadType"))
                    downloadurl=du1+du3
                    tweetlist.removeAt(0)
                    Toast.makeText(applicationContext,"Image Ready to Upload", Toast.LENGTH_LONG).show()
                    adapter!!.notifyDataSetChanged()

                    //Toast.makeText(applicationContext,"url"+downloadurl, Toast.LENGTH_LONG).show()
                 }
            }
                /*addOnCompleteListener{
                task->
                if(task.isSuccessful)
                {
                    val downloadUri=task.result
                    downloadurl=downloadUri!!.toString().substring(0,downloadUri.toString().indexOf("&token"))
                }
            }*/

        }
    fun SplitString(email:String): String {
        val split=email.split("@")
        return split[0]


    }
    fun loadpost()
    {
        myRef.child("posts")
            .addValueEventListener(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onDataChange(DataSnapshot: DataSnapshot) {
                try{
                    tweetlist.clear()
                    tweetlist.add(ticket("0", "New Post", "url", "add"))
                    tweetlist.add(ticket("0", "New Post", "url", "rev"))

                    var td=DataSnapshot!!.value as HashMap <String,Any>
                    for (key in td.keys)
                    {
                        var post=td[key] as HashMap<String,Any>
                        tweetlist.add(ticket(key,post["text"] as String ,post["postImage"] as String,post["userId"] as String ))

                    }
                    adapter!!.notifyDataSetChanged()
                }catch (ex:Exception){}
                }

            })

                }

}
