package hu.ait.android.memeexchange

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import hu.ait.android.memeexchange.adapter.PostsAdapter
import hu.ait.android.memeexchange.data.Post
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        val KEY_USER_ID = "KEY_USER_ID"
        val REQUEST_DETAILS = 1001
        val KEY_POST_ID = "KEY_POST_ID"
        var userID = ""
    }
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postsListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userID = FirebaseAuth.getInstance().currentUser!!.uid

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            startActivity(Intent(
                this@MainActivity, CreatePostActivity::class.java))
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        postsAdapter = PostsAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerPosts.layoutManager = layoutManager
        recyclerPosts.adapter = postsAdapter

        initPosts()
    }

    fun initPosts() {
        val db = FirebaseFirestore.getInstance()
        val postsCollection = db.collection("posts")

        postsListener = postsCollection.addSnapshotListener(object: EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?, p1: FirebaseFirestoreException?) {
                if (p1 != null) {
                    Toast.makeText(this@MainActivity, "Error: ${p1.message}",
                        Toast.LENGTH_LONG).show()
                    return
                }

                for (docChange in querySnapshot!!.getDocumentChanges()) {
                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            val post = docChange.document.toObject(Post::class.java)
                            postsAdapter.addPost(post, docChange.document.id)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val post = docChange.document.toObject(Post::class.java)
                            postsAdapter.updatePost(post, docChange.document.id)
                        }
                        DocumentChange.Type.REMOVED -> {
                            postsAdapter.removePostByKey(docChange.document.id)
                        }
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        postsListener.remove()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_portfolio -> {
                val intentPortfolio = Intent()
                intentPortfolio.setClass(this@MainActivity, PortfolioActivity::class.java)
                startActivityForResult(intentPortfolio, REQUEST_DETAILS)
            }
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                finish()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun openPost(postID : String) {
        val intentPostView = Intent()
        intentPostView.setClass(this@MainActivity, PostViewActivity::class.java)
        intentPostView.putExtra(KEY_POST_ID, postID)
        startActivityForResult(intentPostView, REQUEST_DETAILS)
    }
}
