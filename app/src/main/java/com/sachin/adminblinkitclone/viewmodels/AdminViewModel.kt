package com.sachin.adminblinkitclone.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sachin.adminblinkitclone.models.CartProducts
import com.sachin.adminblinkitclone.models.Orders
import com.sachin.adminblinkitclone.models.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

class AdminViewModel : ViewModel() {

    private val _isImageUploaded = MutableStateFlow<Boolean?>(null)
    var isImageUpload: StateFlow<Boolean?> = _isImageUploaded

    private val _isProductSaved = MutableStateFlow<Boolean?>(null)
    var isProductSaved: StateFlow<Boolean?> = _isProductSaved

    private val _downloadedUrls = MutableStateFlow<ArrayList<String?>>(arrayListOf())
    var downloadedUrls: StateFlow<ArrayList<String?>> = _downloadedUrls

    fun saveImageInDB(imageUris: ArrayList<Uri>) {
        val downloadUrls = ArrayList<String?>()
        var uploadCount = 0
        var errorCount = 0

        if (imageUris.isEmpty()) {
            Log.e("AdminViewModel", "Image URI list is empty")
            _isImageUploaded.value = false
            return
        }

        // Reset trạng thái trước khi upload
        _isImageUploaded.value = null
        _downloadedUrls.value = arrayListOf()

        imageUris.forEach { uri ->
            Log.d("AdminViewModel", "Uploading image: $uri")
            MediaManager.get().upload(uri)
                .unsigned("my_upload_preset_withfirebase")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("AdminViewModel", "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["url"].toString()
                        Log.d("AdminViewModel", "Upload success: $url")
                        downloadUrls.add(url)
                        uploadCount++
                        if (uploadCount + errorCount == imageUris.size) {
                            if (errorCount == 0) {
                                _downloadedUrls.value = downloadUrls
                                _isImageUploaded.value = true
                            } else {
                                _isImageUploaded.value = false
                            }
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("AdminViewModel", "Upload error: ${error.description}, Code: ${error.code}")
                        errorCount++
                        if (uploadCount + errorCount == imageUris.size) {
                            _isImageUploaded.value = false
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.d("AdminViewModel", "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        }
    }

    fun saveProduct(product: Product) {
        // Reset trạng thái trước khi lưu
        _isProductSaved.value = null
        Log.d("AdminViewModel", "Saving product: ${product.productRandomId}")
        Log.d("AdminViewModel", "Product data: $product")
        FirebaseDatabase.getInstance().getReference("Admins")
            .child("AllProduct/${product.productRandomId}").setValue(product)
            .addOnSuccessListener {
                Log.d("AdminViewModel", "Saved to AllProduct")
                FirebaseDatabase.getInstance().getReference("Admins")
                    .child("ProductCategory/${product.productCategory}/${product.productRandomId}").setValue(product)
                    .addOnSuccessListener {
                        Log.d("AdminViewModel", "Saved to ProductCategory")
                        FirebaseDatabase.getInstance().getReference("Admins")
                            .child("ProductType/${product.productType}/${product.productRandomId}").setValue(product)
                            .addOnSuccessListener {
                                Log.d("AdminViewModel", "Saved to ProductType")
                                _isProductSaved.value = true
                            }
                            .addOnFailureListener { e ->
                                Log.e("AdminViewModel", "Failed to save to ProductType: ${e.message}")
                                _isProductSaved.value = false
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AdminViewModel", "Failed to save to ProductCategory: ${e.message}")
                        _isProductSaved.value = false
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AdminViewModel", "Failed to save to AllProduct: ${e.message}")
                _isProductSaved.value = false
            }
    }

    fun fetchAllTheProduct(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProduct")
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) {
                    val prod = product.getValue(Product::class.java)
                    if (category == "All" || prod?.productCategory == category) {
                        prod?.let { products.add(it) }
                    }
                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminViewModel", "Fetch products error: ${error.message}")
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }

    fun logOutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    fun saveingUpdateProducts(product: Product) {
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProduct/${product.productRandomId}").setValue(product)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productRandomId}").setValue(product)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productRandomId}").setValue(product)
    }

    fun updateOrderStatus(orderId: String, status: Int) {
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId).child("orderStatus").setValue(status)
    }

    fun getOrderedProducts(orderId: String): Flow<List<CartProducts>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId)
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Orders::class.java)
                trySend(order?.orderList ?: emptyList())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminViewModel", "Get ordered products error: ${error.message}")
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }

    fun getAllOrders(): Flow<List<Orders>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").orderByChild("orderStatus")
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orderList = ArrayList<Orders>()
                for (orders in snapshot.children) {
                    val order = orders.getValue(Orders::class.java)
                    if (order != null) {
                        orderList.add(order)
                    }
                }
                trySend(orderList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminViewModel", "Get all orders error: ${error.message}")
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { db.removeEventListener(eventListener) }
    }
}