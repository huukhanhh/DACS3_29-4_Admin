package com.sachin.adminblinkitclone.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.models.SlideModel
import com.sachin.adminblinkitclone.FilteringProducts
import com.sachin.adminblinkitclone.R
import com.sachin.adminblinkitclone.databinding.ItemViewProductBinding
import com.sachin.adminblinkitclone.models.Product

class AdapterProduct(
    val onEditButtonClicked: (Product) -> Unit
) : RecyclerView.Adapter<AdapterProduct.ProductViewHolder>(), Filterable {

    class ProductViewHolder(val binding: ItemViewProductBinding) : RecyclerView.ViewHolder(binding.root)

    private val diffUtil = object : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productRandomId == newItem.productRandomId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(ItemViewProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = differ.currentList[position]
        holder.binding.apply {
            val productImageUris = product.productImageUris

            // Debug và xử lý ảnh với ImageSlideshow
            if (productImageUris != null && productImageUris.isNotEmpty()) {
                Log.d("AdapterProduct", "Product Image URLs: $productImageUris")
                val imageList = ArrayList<SlideModel>()
                try {
                    for (i in productImageUris.indices) {
                        val url = productImageUris[i]
                        if (url != null) {
                            val secureUrl = if (url.startsWith("http://")) {
                                Log.w("AdapterProduct", "Converting http to https: $url")
                                url.replace("http://", "https://")
                            } else {
                                url
                            }
                            imageList.add(SlideModel(secureUrl))
                            Log.d("AdapterProduct", "Adding URL to ImageSlideshow: $secureUrl")
                        } else {
                            Log.w("AdapterProduct", "Null URL at index $i for product: ${product.productTitle}")
                            imageList.add(SlideModel("https://via.placeholder.com/300?text=Null+URL"))
                        }
                    }
                    ivImageSlider.setImageList(imageList)
                    Log.d("AdapterProduct", "ImageSlideshow set with ${imageList.size} images")
                } catch (e: Exception) {
                    Log.e("AdapterProduct", "ImageSlideshow error: ${e.message}")
                    if (productImageUris.isNotEmpty()) {
                        Glide.with(holder.itemView.context)
                            .load(productImageUris[0])
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(holder.itemView.findViewById<ImageView>(R.id.ivImagePreview))
                    }
                }
            } else {
                Log.w("AdapterProduct", "No image URLs for product: ${product.productTitle}")
                ivImageSlider.setImageList(listOf(SlideModel("https://via.placeholder.com/300?text=No+Image")))
            }

            tvProductTitle.text = product.productTitle
            val quantity = "${product.productQuantity} ${product.productUnit}"
            tvProductQuantity.text = quantity
            tvProductPrice.text = "₹${product.productPrice}"
        }
        holder.itemView.findViewById<TextView>(R.id.tvAdd).setOnClickListener {
            onEditButtonClicked(product)
        }
    }

    val filter: FilteringProducts? = null
    var orginalList = ArrayList<Product>()
    override fun getFilter(): Filter {
        return filter ?: FilteringProducts(this, orginalList)
    }
}