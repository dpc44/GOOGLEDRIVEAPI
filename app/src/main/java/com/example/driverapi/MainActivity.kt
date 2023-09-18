package com.example.driverapi

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.driverapi.Adapter.ArticleAdapter
import com.example.driverapi.Data.ArticleData
import com.example.driverapi.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.drive.DriveFile
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_SIGN_IN = 101
    private val REQUEST_REMOTE_CONSENT = 102

    // -----------------------------------------------------------
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestSignIn()
        // After change -----------



    }


    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()
        val signInClient = GoogleSignIn.getClient(this, signInOptions)
        val signInIntent = signInClient.signInIntent
        startActivityForResult(signInIntent, 400)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("resultCode", "$resultCode")
        when (requestCode) {
            400 ->
                if (resultCode == RESULT_OK) {
                    handleSignInIntent(data)
                }
        }
    }

    private fun handleSignInIntent(data: Intent?) {

        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener { account ->
                val credentials = GoogleAccountCredential.usingOAuth2(
                    applicationContext,
                    listOf(DriveScopes.DRIVE)
                )
                credentials.selectedAccount = account.account!!

                val drive = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credentials
                )
                    .setApplicationName("my-drive") // Replace with your app name
                    .build()


                Log.d("SignedInAccount", "Email: ${account.email}")

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val result = drive.files().list()
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name, mimeType)")
                            .setQ("name = 'news.json'")
                            .execute()

                        if (result.files.isNotEmpty()) {
                            val fileId = result.files[0].id
                            val outputStream = ByteArrayOutputStream()
                            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                            val jsonContent = String(outputStream.toByteArray())
                            val jsonArray = JSONArray(jsonContent)

                            val articleList = mutableListOf<ArticleData>()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val image = jsonObject.getString("Image")
                                val link = jsonObject.getString("Link")
                                val title = jsonObject.getString("Title")
                                val article = ArticleData(image, link, title)
                                articleList.add(article)
                            }
                            withContext(Dispatchers.Main) {
                                val recyclerView = binding.jsonRecycler
                                recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                                val adapter = ArticleAdapter(this@MainActivity, articleList)
                                recyclerView.adapter = adapter
                            }
                            //Log.d("articleList",articleList.toString())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }





                Log.d("test", "test")
                Toast.makeText(this, "Connected to Google Drive", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                val errorMessage = "Failed to connect to Google Drive: ${e.message}"
                val accountInfo = GoogleSignIn.getSignedInAccountFromIntent(data).toString()
                Log.e("GoogleDriveConnection", "$errorMessage, Account Info: $accountInfo")
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
    }


}