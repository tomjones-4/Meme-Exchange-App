package hu.ait.android.memeexchange

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.android.memeexchange.MainActivity.Companion.KEY_POST_ID
import hu.ait.android.memeexchange.MainActivity.Companion.userID

import hu.ait.android.memeexchange.data.Share
import hu.ait.android.memeexchange.data.User
import kotlinx.android.synthetic.main.buy_dialog.*
import kotlinx.android.synthetic.main.buy_dialog.view.*
import java.text.DecimalFormat

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class TransactionDialog : DialogFragment(), AdapterView.OnItemSelectedListener {

    private var buyingPower = 0.0

    private val df = DecimalFormat("#.##")

    private lateinit var tvBuyingPower: TextView
    private lateinit var etQuantity: EditText
    private lateinit var tvCurrQuantity: TextView

    private var currQuantity = 0

    private var postID = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext())

        val rootView = requireActivity().layoutInflater.inflate(R.layout.buy_dialog, null)

        tvBuyingPower = rootView.tvBuyingPower
        etQuantity = rootView.etQuantity
        tvCurrQuantity = rootView.tvCurrQuantity

        Thread {
            FirebaseFirestore.getInstance().collection("users").document(userID).get()
                    .addOnSuccessListener { documentSnapshot ->
                        buyingPower = documentSnapshot.get("buyingPower").toString().toDouble()
                        tvBuyingPower.text = buyingPower.toString()
                    }
        }.start()


        builder.setView(rootView)

        val arguments = this.arguments

        if (arguments != null && arguments.containsKey("KEY_ITEM_TO_BUY")) {
            builder.setTitle("Buying a post")
            builder.setPositiveButton("Buy post") { dialog, witch ->
            }
        } else {
            builder.setTitle("Selling a post")
            builder.setPositiveButton("Sell post") { dialog, witch ->
            }
        }

        if (arguments?.get(KEY_POST_ID) != null) {
            postID = arguments.get(KEY_POST_ID).toString()
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(userID)
                .collection("owned_posts")
                .document(postID).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        currQuantity = documentSnapshot.get("quantity").toString().toInt()
                        tvCurrQuantity.text = currQuantity.toString()
                    } else {
                        tvCurrQuantity.text = "0"
                    }
                }

        return builder.create()
    }


    override fun onResume() {
        super.onResume()

        val positiveButton = (dialog as AlertDialog).getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            if (etQuantity.text.isNotEmpty()) {
                val arguments = this.arguments
                var ownedPostRef = FirebaseFirestore.getInstance().collection("users")
                        .document(userID)
                        .collection("owned_posts")
                        .document(postID)

                var userRef = FirebaseFirestore.getInstance().collection("users").document(userID)

                var postRef = FirebaseFirestore.getInstance().collection("posts")
                        .document(postID)


                if (arguments != null && arguments.containsKey("KEY_ITEM_TO_BUY")) {
                    // handle buy
                    handleBuy(postRef, userRef, ownedPostRef)


                } else {
                    // handle sell
                    handleSell(postRef, userRef, ownedPostRef)

                }


            } else {
                etQuantity.error = "This field can not be empty"
            }
        }
    }

    fun handleSell(postRef: DocumentReference, userRef: DocumentReference, ownedPostRef: DocumentReference) {
        var quantity = etQuantity.text.toString().toInt()

        if (quantity > currQuantity) {
            etQuantity.error = "Cannot sell more than you own"
        } else {

            var soldEquity = 0.0

            postRef.get().addOnSuccessListener { documentSnapshot ->
                soldEquity = documentSnapshot.get("score").toString().toDouble() * quantity
                userRef.update("buyingPower", buyingPower + soldEquity)
            }

            if (quantity == currQuantity) {
                ownedPostRef.delete()
                postRef.collection("owners").document(userID).delete()
            } else {
                ownedPostRef.update("quantity", currQuantity - quantity)
            }
            dialog.dismiss()
        }
    }

    fun handleBuy(postRef: DocumentReference, userRef: DocumentReference, ownedPostRef: DocumentReference) {
        val quantity = etQuantity.text.toString().toInt()

        postRef.get().addOnSuccessListener { documentSnapshot ->
            val marketPrice = documentSnapshot.get("score").toString().toDouble()
            val boughtEquity = quantity * marketPrice

            if (buyingPower < boughtEquity) {
                etQuantity.error = "Not enough buying power"
            } else {
                userRef.collection("owned_posts").document(postID).get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (!documentSnapshot.exists()) {
                                handleNewShare(marketPrice, quantity, userRef)
                            } else {
                                handleAlreadyBoughtShare(ownedPostRef, postRef, quantity, marketPrice)
                            }
                            buyingPower -= marketPrice * quantity
                            userRef.update("buyingPower", buyingPower)
                            postRef.update("owners", FieldValue.arrayUnion(userID))
                        }
            }
        }
    }

    fun handleAlreadyBoughtShare(ownedPostRef: DocumentReference, postRef: DocumentReference, quantity: Int, marketPrice: Double) {
        var currQuantity = 0
        var avgCost = 0.0
        ownedPostRef.get().addOnSuccessListener { documentSnapshot ->
            currQuantity = documentSnapshot.get("quantity").toString().toInt()
            avgCost = documentSnapshot.get("avgCost").toString().toDouble()
            ownedPostRef.update("quantity", (currQuantity + quantity))
            var totalQuantity = currQuantity + quantity
            avgCost = df.format((avgCost * currQuantity + marketPrice * quantity) / totalQuantity).toDouble()
            ownedPostRef.update("avgCost", avgCost)
            dialog.dismiss()
        }

    }

    fun handleNewShare(marketPrice: Double, quantity: Int, userRef: DocumentReference) {
        val boughtShare = Share(
                postID,
                marketPrice,
                quantity
        )

        userRef.collection("owned_posts").document(postID).set(boughtShare)
                .addOnSuccessListener {
                    dialog.dismiss()
                }

    }

    override fun onDismiss(dialog: DialogInterface?) {
        var ownedPostRef = FirebaseFirestore.getInstance().collection("users")
                .document(userID)
                .collection("owned_posts")
                .document(postID)

        var postRef = FirebaseFirestore.getInstance().collection("posts")
                .document(postID)

        (context as PostViewActivity).updatePostDetails(postRef, ownedPostRef)
        super.onDismiss(dialog)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    }

}