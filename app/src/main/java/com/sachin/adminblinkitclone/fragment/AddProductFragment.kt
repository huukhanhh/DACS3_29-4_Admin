package com.sachin.adminblinkitclone.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sachin.adminblinkitclone.Constants
import com.sachin.adminblinkitclone.R
import com.sachin.adminblinkitclone.Utils
import com.sachin.adminblinkitclone.activity.AdminMainActivity
import com.sachin.adminblinkitclone.adapters.AdapterSelectedImage
import com.sachin.adminblinkitclone.databinding.FragmentAddProductBinding
import com.sachin.adminblinkitclone.models.Product
import com.sachin.adminblinkitclone.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AddProductFragment : Fragment() {

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var binding: FragmentAddProductBinding
    private val imageUris: ArrayList<Uri> = arrayListOf()
    val selectedImage =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { listOfUri ->
            val fiveImages = listOfUri.take(5)
            imageUris.clear()
            imageUris.addAll(fiveImages)
            binding.rvProductImage.adapter = AdapterSelectedImage(imageUris)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddProductBinding.inflate(inflater, container, false)
        setStatusBarColor()
        setAutoCompleteTextViews()
        onImageSelectClicked()
        onAddButtonClicked()
        return binding.root
    }

    private fun onAddButtonClicked() {
        binding.btnAddProduct.setOnClickListener {
            // Vô hiệu hóa nút ngay khi nhấn
            binding.btnAddProduct.isEnabled = false
            Utils.showDialog(requireContext(), "Uploading images...")

            val productTitle = binding.etProductTitle.text.toString()
            val productQuantity = binding.etProductQuantity.text.toString()
            val productUnit = binding.etProductUnit.text.toString()
            val productPrice = binding.etProductPrice.text.toString()
            val productStock = binding.etProductStock.text.toString()
            val productCategory = binding.etProductCategory.text.toString()
            val productType = binding.etProductType.text.toString()

            if (productTitle.isEmpty() || productQuantity.isEmpty() || productUnit.isEmpty() || productPrice.isEmpty() || productStock.isEmpty() ||
                productCategory.isEmpty() || productType.isEmpty()
            ) {
                Utils.apply {
                    hideDialog()
                    showToast(requireContext(), message = "Empty fields are not allowed")
                }
                // Kích hoạt lại nút nếu kiểm tra thất bại
                binding.btnAddProduct.isEnabled = true
            } else if (imageUris.isEmpty()) {
                Utils.apply {
                    hideDialog()
                    showToast(requireContext(), message = "Please upload some image")
                }
                // Kích hoạt lại nút nếu không có ảnh
                binding.btnAddProduct.isEnabled = true
            } else {
                val product = Product(
                    productTitle = productTitle,
                    productQuantity = productQuantity.toInt(),
                    productUnit = productUnit,
                    productPrice = productPrice.toInt(),
                    productStock = productStock.toInt(),
                    productCategory = productCategory,
                    productType = productType,
                    itemCount = 0,
                    adminUid = Utils.getCurrentUserId(),
                    productRandomId = Utils.getRandomId()
                )
                saveImage(product)
            }
        }
    }

    private fun saveImage(product: Product) {
        viewModel.saveImageInDB(imageUris)
        lifecycleScope.launch {
            viewModel.isImageUpload.collect { isUploaded ->
                if (isUploaded == null) return@collect // Bỏ qua trạng thái ban đầu
                if (isUploaded) {
                    Utils.apply {
                        hideDialog()
                        showToast(requireContext(), "Images uploaded successfully")
                    }
                    getUrls(product)
                } else {
                    Utils.apply {
                        hideDialog()
                        showToast(requireContext(), "Failed to upload images")
                    }
                    // Kích hoạt lại nút nếu upload thất bại
                    binding.btnAddProduct.isEnabled = true
                }
            }
        }
    }

    private fun getUrls(product: Product) {
        Utils.showDialog(requireContext(), "Publishing product...")
        lifecycleScope.launch {
            viewModel.downloadedUrls.collect { urls ->
                if (urls.isEmpty()) return@collect // Bỏ qua trạng thái ban đầu
                product.productImageUris = urls
                saveProduct(product)
            }
        }
    }

    private fun saveProduct(product: Product) {
        viewModel.saveProduct(product)
        lifecycleScope.launch {
            viewModel.isProductSaved.collect { isSaved ->
                if (isSaved == null) return@collect // Bỏ qua trạng thái ban đầu
                if (isSaved) {
                    Utils.apply {
                        hideDialog()
                        showToast(requireContext(), message = "Your product is live")
                    }
                    startActivity(Intent(requireActivity(), AdminMainActivity::class.java))
                    requireActivity().finish()
                    // Kích hoạt lại nút sau khi thêm thành công
                    binding.btnAddProduct.isEnabled = true
                } else {
                    Utils.apply {
                        hideDialog()
                        showToast(requireContext(), "Failed to save product")
                    }
                    // Kích hoạt lại nút nếu lưu thất bại
                    binding.btnAddProduct.isEnabled = true
                }
            }
        }
    }

    private fun onImageSelectClicked() {
        binding.btnSelectImage.setOnClickListener {
            selectedImage.launch("image/*")
        }
    }

    private fun setAutoCompleteTextViews() {
        val units = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allUnitsOfProducts)
        val category = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductCategory)
        val productType = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductType)

        binding.apply {
            etProductUnit.setAdapter(units)
            etProductCategory.setAdapter(category)
            etProductType.setAdapter(productType)
        }
    }

    private fun setStatusBarColor() {
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.yellow)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}