package hu.ait.android.memeexchange

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import hu.ait.android.memeexchange.data.Post
import kotlinx.android.synthetic.main.activity_create_post.*
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*

class CreatePostActivity : AppCompatActivity() {
    companion object {
        private val PERMISSION_REQUEST_CODE = 101
        private const val CAMERA_REQUEST_CODE = 102
        private const val GET_FROM_GALLERY = 27

    }

    var uploadBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)
        title = "Create a Post"
        btnSend.setOnClickListener {
            if (uploadBitmap != null) {
                uploadPostWithImage()
            } else {
                //uploadPost()

                Toast.makeText(this,
                        "Please attach image to post", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            finish()
        }
        btnAttach.setOnClickListener {
            //startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE)
            startActivityForResult(Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), GET_FROM_GALLERY)

        }

        btnAttach.isEnabled = false
        requestNeededPermission()
    }

    private fun requestNeededPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this,
                    "I need it for gallery", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE)
        } else {
            // már van engedély
            btnAttach.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Gallery perm granted", Toast.LENGTH_SHORT).show()
                    btnAttach.isEnabled = true
                } else {
                    Toast.makeText(this, "Gallery perm NOT granted", Toast.LENGTH_SHORT).show()
                    btnAttach.isEnabled = false
                }
            }
        }
    }

    private fun uploadPost(imgUrl: String = "") {
        val post = Post(
            FirebaseAuth.getInstance().currentUser!!.uid,
            FirebaseAuth.getInstance().currentUser!!.displayName!!,
            etTitle.text.toString(),
            0,
            imgUrl, ArrayList()
        )

        val postsCollections = FirebaseFirestore.getInstance().collection("posts")

        postsCollections.add(post)
            .addOnSuccessListener {
                Toast.makeText(this@CreatePostActivity, "Post saved",
                    Toast.LENGTH_LONG).show()
            }.addOnFailureListener{
                Toast.makeText(this@CreatePostActivity, "Error ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            val selectedImage = data!!.getData()
            data?.let {
                uploadBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage) as Bitmap
                imgAttach.setImageBitmap(uploadBitmap)
                imgAttach.visibility = View.VISIBLE
            }
        }
    }

    @Throws(Exception::class)
    private fun uploadPostWithImage() {
        val baos = ByteArrayOutputStream()
        uploadBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageInBytes = baos.toByteArray()

        val storageRef = FirebaseStorage.getInstance().getReference()
        val newImage = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8") + ".jpg"
        val newImagesRef = storageRef.child("images/$newImage")

        newImagesRef.putBytes(imageInBytes)
            .addOnFailureListener { exception ->
                Toast.makeText(this@CreatePostActivity, exception.message, Toast.LENGTH_SHORT).show()
            }.addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                newImagesRef.downloadUrl.addOnCompleteListener(object: OnCompleteListener<Uri> {
                    override fun onComplete(task: Task<Uri>) {
                        uploadPost(task.result.toString())
                    }
                })
            }
    }
}
