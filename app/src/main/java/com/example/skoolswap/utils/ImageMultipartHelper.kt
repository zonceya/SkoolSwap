package com.example.skoolswap.utils

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

object ImageMultipartHelper {

    fun createImagePart(context: Context, uri: Uri, partName: String = "images[]"): MultipartBody.Part? {
        return try {
            // Read the file
            val file = getFileFromUri(context, uri) ?: return null

            // Create request body
            val requestBody = RequestBody.create(
                "image/*".toMediaTypeOrNull(),
                file
            )

            // Create multipart part
            MultipartBody.Part.createFormData(
                partName,
                file.name,
                requestBody
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createImageParts(context: Context, uris: List<Uri>): List<MultipartBody.Part> {
        return uris.mapNotNull { uri ->
            createImagePart(context, uri)
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile("item_image_", ".jpg", context.cacheDir)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}