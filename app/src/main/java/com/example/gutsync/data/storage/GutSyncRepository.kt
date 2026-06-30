package com.example.gutsync.data.storage

import android.content.Context
import android.util.Log
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.auth.AuthSession
import com.example.gutsync.data.auth.SessionManager
import com.google.android.gms.tasks.Tasks
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GutSyncRepository(private val context: Context) {
    private val _appData = MutableStateFlow(AppData())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    private val localFile = File(context.filesDir, "gutsync_data.json")
    private val sessionManager = SessionManager(context)
    private var driveHelper: DriveServiceHelper? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadData()
        }
    }

    fun setDriveService(drive: Drive) {
        driveHelper = DriveServiceHelper(drive)
    }

    suspend fun loadData() = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getSession()
            if (session.isDriveConnected && driveHelper != null) {
                val driveContent = Tasks.await(driveHelper!!.readFile(session.driveFileId!!))
                _appData.value = Json.decodeFromString<AppData>(driveContent)
            } else if (localFile.exists()) {
                val jsonStr = localFile.readText()
                _appData.value = Json.decodeFromString<AppData>(jsonStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveData() = withContext(Dispatchers.IO) {
        try {
            val jsonStr = Json.encodeToString(_appData.value)
            localFile.writeText(jsonStr)

            val session = sessionManager.getSession()
            if (session.isDriveConnected && driveHelper != null) {
                Tasks.await(driveHelper!!.updateFile(session.driveFileId!!, jsonStr))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWithDrive(drive: Drive): AuthSession = withContext(Dispatchers.IO) {
        val helper = DriveServiceHelper(drive)
        driveHelper = helper
        val session = sessionManager.getSession()
        
        Log.d("GutSyncAuth", "Starting syncWithDrive process on IO thread...")
        
        try {
            // 1. Search for GutSync folder
            Log.d("GutSyncAuth", "Searching for GutSync folder...")
            var folderId = Tasks.await(helper.searchFile("GutSync"))
            if (folderId == null) {
                Log.d("GutSyncAuth", "Folder not found. Creating GutSync folder...")
                folderId = Tasks.await(helper.createFolder("GutSync"))
                Log.d("GutSyncAuth", "Folder created with ID: $folderId")
            } else {
                Log.d("GutSyncAuth", "Found existing folder ID: $folderId")
            }

            // 2. Search for data file in folder
            Log.d("GutSyncAuth", "Searching for gutsync_data.json in folder...")
            var fileId = Tasks.await(helper.searchFileInFolder(folderId!!, "gutsync_data.json"))
            
            if (fileId == null) {
                Log.d("GutSyncAuth", "File not found. Creating new data file...")
                val jsonStr = Json.encodeToString(_appData.value)
                fileId = Tasks.await(helper.createFileInFolder(folderId, "gutsync_data.json", jsonStr))
                Log.d("GutSyncAuth", "File created with ID: $fileId")
            } else {
                Log.d("GutSyncAuth", "Found existing file ID: $fileId. Loading content...")
                val driveContent = Tasks.await(helper.readFile(fileId))
                _appData.value = Json.decodeFromString<AppData>(driveContent)
                Log.d("GutSyncAuth", "Data loaded from Drive successfully.")
            }

            val updatedSession = session.copy(
                driveFolderId = folderId,
                driveFileId = fileId
            )
            sessionManager.saveSession(updatedSession)
            Log.d("GutSyncAuth", "Session updated and saved with Drive IDs.")
            updatedSession
        } catch (e: Exception) {
            Log.e("GutSyncAuth", "FATAL ERROR during Drive Sync: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun addMeal(nutrients: NutrientData) {
        val currentMeals = _appData.value.meals.toMutableList()
        currentMeals.add(MealLogEntry(nutrients))
        _appData.value = _appData.value.copy(meals = currentMeals)
        saveData()
    }

    suspend fun addChatMessage(message: ChatMessage) {
        val currentChats = _appData.value.chats.toMutableList()
        currentChats.add(message)
        _appData.value = _appData.value.copy(chats = currentChats)
        saveData()
    }

    suspend fun updateProfile(profile: UserProfile) {
        _appData.value = _appData.value.copy(profile = profile)
        saveData()
    }
}
