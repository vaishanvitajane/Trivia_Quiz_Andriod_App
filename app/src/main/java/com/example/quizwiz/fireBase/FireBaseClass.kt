package com.example.quizwiz.fireBase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.example.quizwiz.constants.Base
import com.example.quizwiz.constants.Constants
import com.example.quizwiz.models.LeaderBoardModel
import com.example.quizwiz.models.UserModel
import com.google.android.gms.tasks.Task
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class FireBaseClass {
    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(userInfo: UserModel) {
        mFireStore.collection(Constants.user)
            .document(getCurrentUserId()).set(userInfo, SetOptions.merge())
    }

    fun getUserInfo(callback: UserInfoCallback) {
        val userDocument =
            FirebaseFirestore.getInstance().collection(Constants.user).document(getCurrentUserId())

        userDocument.get().addOnSuccessListener { documentSnapshot ->
            val userInfo = documentSnapshot.toObject(UserModel::class.java)
            callback.onUserInfoFetched(userInfo)
        }.addOnFailureListener { e ->
            // Handle the error here
            callback.onUserInfoFetched(null)
        }
    }

    fun updateProfile(name:String?, imgUri: Uri?)
    {
        val userDocument = mFireStore.collection(Constants.user).document(getCurrentUserId())
        if (!name.isNullOrEmpty())
        {
            userDocument.update("name",name)
        }
        if (imgUri!=null)
        {
            uploadImage(imgUri)
        }
    }

    fun setProfileImage(imageRef:String?,view: ShapeableImageView)
    {
        if (!imageRef.isNullOrEmpty()) {
            val storageRef = FirebaseStorage.getInstance().reference
            val pathReference = storageRef.child(imageRef)
            val ONE_MEGABYTE: Long = 1024 * 1024
            pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { byteArray ->
                val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                view.setImageBitmap(bmp)
            }
        }
    }

    private fun uploadImage(imgUri: Uri)
    {
        val userDocument = mFireStore.collection(Constants.user).document(getCurrentUserId())
        val storageRef = FirebaseStorage.getInstance().reference
        val profilePicRef = storageRef.child("profile_pictures/${getCurrentUserId()}")
        val uploadTask = profilePicRef.putFile(imgUri)
        uploadTask.addOnCompleteListener{task->
            if (task.isSuccessful){
                userDocument.update("image","profile_pictures/${getCurrentUserId()}")
            }
            else{
                Log.e("ImageUpload","Unsuccessful")
            }
        }
    }

    fun updateScore(newScore: Double) {
        val userDocument = mFireStore.collection(Constants.user).document(getCurrentUserId())
        getUserInfo(object : UserInfoCallback {
            override fun onUserInfoFetched(userInfo: UserModel?) {
                userInfo?.let {
                    val newAllTimeScore = userInfo.allTimeScore + newScore
                    val newWeeklyScore = userInfo.weeklyScore + newScore
                    val newMonthlyScore = userInfo.monthlyScore + newScore
                    userDocument.update(
                        Constants.allTimeScore, newAllTimeScore,
                        Constants.weeklyScore, newWeeklyScore,
                        Constants.monthlyScore, newMonthlyScore,
                        Constants.lastGameScore, newScore
                    ).addOnSuccessListener {
                        Log.e("DataUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.e("DataUpdate", "Failed")
                    }
                }
            }
        })
    }

    fun getUserRank(type: String,callback:UserRankCallback){
        var rank: Int? = null
        mFireStore.collection(Constants.user).orderBy(type, Query.Direction.DESCENDING)
            .get().addOnSuccessListener { result ->
                rank = 1
                val usrId = getCurrentUserId()
                for (document in result) {
                    if (document.id == usrId)
                        break
                    rank = rank!! + 1
                }
                callback.onUserRankFetched(rank)
            }.addOnFailureListener {
                Log.e("QueryResult", "Failure")
                callback.onUserRankFetched(null)
            }
    }

    fun getLeaderBoardData(type: String,callBack:LeaderBoardDataCallback) {
        mFireStore.collection(Constants.user)
            .orderBy(type, Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener {
                val otherRankers = mutableListOf<UserModel?>()
                val rank1 = it.documents[0].toObject(UserModel::class.java)
                val rank2 = it.documents[1].toObject(UserModel::class.java)
                val rank3 = it.documents[2].toObject(UserModel::class.java)

                for (i in 3 until it.documents.size) {
                    val userInfo = it.documents[i].toObject(UserModel::class.java)
                    otherRankers.add(userInfo)
                }
                callBack.onLeaderBoardDataFetched(
                    LeaderBoardModel(rank1,rank2,rank3,otherRankers)
                )
            }.addOnFailureListener {
                callBack.onLeaderBoardDataFetched(null)
            }
    }

    fun doesDocumentExist(documentId: String): Task<Boolean> {
        val db = FirebaseFirestore.getInstance()
        val documentRef = db.collection(Constants.user).document(documentId)

        return documentRef.get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    task.result?.exists() ?: false
                } else {
                    // Handle the error
                    false
                }
            }
    }

    fun getCurrentUserId(): String {
        val currentUser = Firebase.auth.currentUser
        var currentUserId = ""
        if (currentUser != null)
            currentUserId = currentUser.uid
        return currentUserId
    }

    interface UserInfoCallback {
        fun onUserInfoFetched(userInfo: UserModel?)
    }

    interface UserRankCallback {
        fun onUserRankFetched(rank:Int?)
    }

    interface LeaderBoardDataCallback{
        fun onLeaderBoardDataFetched(leaderBoardModel: LeaderBoardModel?)
    }
}