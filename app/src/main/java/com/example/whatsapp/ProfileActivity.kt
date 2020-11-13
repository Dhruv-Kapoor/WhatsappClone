package com.example.whatsapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.whatsapp.models.User
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_profile.*

const val RC_PICK_IMAGE = 100
const val EMULATORS_ENABLED = false

class ProfileActivity : AppCompatActivity() {

    val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val storageRef by lazy {
        FirebaseStorage.getInstance().reference
    }

    val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private var downloadUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        if(EMULATORS_ENABLED) {
            firestore.useEmulator(getString(R.string.local_host), 5500)
        }
        etName.addTextChangedListener {
            it?.let {
                btnNext.isEnabled = it.isNotEmpty()
            }
        }

        ivUser.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, RC_PICK_IMAGE)
        }

        btnNext.setOnClickListener {
            val token = FirebaseMessaging.getInstance().token.addOnSuccessListener {
                createUser(it)
            }

        }

    }

    private fun createUser(token: String?) {
        var status = "Hey there I am using Whatsapp"
        if(etStatus.text.isNotEmpty()){
            status = etStatus.text.toString()
        }
        val user = User(etName.text.toString(), downloadUrl, downloadUrl, auth.uid!!, token!!, auth.currentUser?.phoneNumber!!, status)
        firestore.collection("users").document(auth.uid!!).set(user)
            .addOnSuccessListener {
                startActivity(
                    Intent(this@ProfileActivity, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                )
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.let {
                    setUserImage(it)
                    uploadImage(it)
                }
            }
        }
    }

    private fun uploadImage(data: Uri) {
        btnNext.isEnabled = false
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE
        val ref = storageRef.child("users").child(auth.uid!!)
        val uploadTask = ref.putFile(data)
        uploadTask.addOnProgressListener {
            val progress = (it.bytesTransferred * 100 / it.totalByteCount).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress(progress, true)
            } else {
                progressBar.progress = progress
            }
        }
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener {
            downloadUrl = it.result.toString()
            btnNext.isEnabled = true
            progressBar.visibility = View.INVISIBLE
        }

    }

    private fun setUserImage(data: Uri) {
        ivUser.setImageURI(data)
    }
}