package com.example.feed

import android.provider.MediaStore

class postInfo
{
    var userId:String?=null
    var text:String?=null
    var postImage:String?=null
    constructor(userId:String,text:String,postImage:String)
    {
        this.userId=userId
        this.text=text
        this.postImage=postImage
    }
}
