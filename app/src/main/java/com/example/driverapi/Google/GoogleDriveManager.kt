import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


class GoogleDriveManager(private val activity: Activity) {


    companion object {
        const val REQUEST_CODE_SIGN_IN = 400
    }

    private lateinit var drive: Drive

    init {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()

        val signInClient = GoogleSignIn.getClient(activity, signInOptions)
        val signInIntent = signInClient.signInIntent
        activity.startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)

        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account != null) {
            val credentials = GoogleAccountCredential.usingOAuth2(
                activity.applicationContext,
                listOf(DriveScopes.DRIVE)
            )
            credentials.selectedAccount = account.account

            drive = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credentials
            )
                .setApplicationName("my-drive") // Replace with your app name
                .build()
        }
    }

    suspend fun listFiles(): FileList? {
        return withContext(Dispatchers.IO) {
            try {
                drive.files().list()
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, mimeType)")
                    .execute()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getFileContent(fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val result: FileList = drive.files().list()
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, mimeType)")
                    .setQ("name = '$fileName'")
                    .execute()

                if (result.files.isNotEmpty()) {
                    val fileId = result.files[0].id
                    val outputStream = ByteArrayOutputStream()
                    drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                    String(outputStream.toByteArray())
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun handleSignInResult(data: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener { account ->
                Log.d("GoogleDriveManager", "Email: ${account.email}")
            }
            .addOnFailureListener { e ->
                Log.e("GoogleDriveManager", "Failed to connect to Google Drive: ${e.message}")
            }
    }
}

