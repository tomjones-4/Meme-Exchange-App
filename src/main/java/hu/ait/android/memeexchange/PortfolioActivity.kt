package hu.ait.android.memeexchange

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import hu.ait.android.memeexchange.MainActivity.Companion.userID
import hu.ait.android.memeexchange.adapter.PostsAdapter
import hu.ait.android.memeexchange.data.Post
import hu.ait.android.memeexchange.data.User
import kotlinx.android.synthetic.main.activity_portfolio.*
import kotlinx.android.synthetic.main.app_bar_main.*

class PortfolioActivity : AppCompatActivity() {

    private lateinit var portfolioPostsAdapter: PostsAdapter
    private lateinit var portfolioPostsListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portfolio)
        title = "My Portfolio"

        Thread {

            FirebaseFirestore.getInstance().collection("users").document(userID).collection("owned_posts")
                    .get()
                    .addOnSuccessListener { result->
                        for (document in result) {
                            val shareQuantity = document.get("quantity").toString().toDouble()
                            var postRef = FirebaseFirestore.getInstance().collection("posts").document(document.id)
                            postRef.get()
                                    .addOnSuccessListener { postDocument ->
                                        val shareEquity = postDocument.get("score").toString().toDouble() * shareQuantity
                                        tvEquity.text = (tvEquity.text.toString().toDouble() + shareEquity).toString()
                                    }
                        }

                        FirebaseFirestore.getInstance().collection("users").document(userID)
                                .get()
                                .addOnSuccessListener { documentSnapshot ->
                                    tvEquity.text = (tvEquity.text.toString().toDouble() + documentSnapshot.get("buyingPower").toString().toDouble()).toString()
                                }

                    }
        }.start()

        setContentView(R.layout.activity_portfolio)
        portfolioPostsAdapter = PostsAdapter(this,
                FirebaseAuth.getInstance().currentUser!!.uid)
        val layoutManager = LinearLayoutManager(this@PortfolioActivity)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerPortfolioPosts.adapter = portfolioPostsAdapter
        recyclerPortfolioPosts.layoutManager = layoutManager


        initPortfolioPosts()
    }

    fun initPortfolioPosts() {

        val db = FirebaseFirestore.getInstance()
        val portfolioCollection = db.collection("posts").whereArrayContains("owners", userID)

        portfolioPostsListener = portfolioCollection.addSnapshotListener(object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?, p1: FirebaseFirestoreException?) {
                if (p1 != null) {
                    Toast.makeText(this@PortfolioActivity, "Error: ${p1.message}",
                            Toast.LENGTH_LONG).show()
                    return
                }

                for (docChange in querySnapshot!!.getDocumentChanges()) {
                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            val post = docChange.document.toObject(Post::class.java)
                            portfolioPostsAdapter.addPost(post, docChange.document.id)
                        }
                        DocumentChange.Type.MODIFIED -> {

                        }
                        DocumentChange.Type.REMOVED -> {
                            portfolioPostsAdapter.removePostByKey(docChange.document.id)
                        }
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        portfolioPostsListener.remove()
        super.onDestroy()
    }

    fun openPost(postID : String) {
        val intentPostView = Intent()
        intentPostView.setClass(this@PortfolioActivity, PostViewActivity::class.java)
        intentPostView.putExtra(MainActivity.KEY_POST_ID, postID)
        startActivityForResult(intentPostView, MainActivity.REQUEST_DETAILS)
    }
}
