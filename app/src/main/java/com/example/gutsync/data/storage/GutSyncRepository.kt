package com.example.gutsync.data.storage

import android.content.Context
import android.util.Log
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.ChatSession
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

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadData()
        }
    }

    fun clearData() {
        _appData.value = AppData()
        driveHelper = null
        if (localFile.exists()) {
            localFile.delete()
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
                _appData.value = json.decodeFromString<AppData>(driveContent)
                // If we have local data but just connected to drive, we might want to clean up local
                if (localFile.exists()) {
                    Log.d("GutSyncAuth", "Cleaning up local file as Drive is connected")
                    localFile.delete()
                }
            } else if (localFile.exists()) {
                val jsonStr = localFile.readText()
                _appData.value = json.decodeFromString<AppData>(jsonStr)
            }
        } catch (e: Exception) {
            Log.e("GutSyncAuth", "Error loading data: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun saveData() = withContext(Dispatchers.IO) {
        try {
            val jsonStr = json.encodeToString(_appData.value)
            val session = sessionManager.getSession()
            
            if (session.isDriveConnected && driveHelper != null) {
                // Store ONLY in Drive
                Tasks.await(driveHelper!!.updateFile(session.driveFileId!!, jsonStr))
                // Ensure local file is gone if it exists
                if (localFile.exists()) {
                    localFile.delete()
                }
            } else {
                // Offline mode: Store locally
                localFile.writeText(jsonStr)
            }
        } catch (e: Exception) {
            Log.e("GutSyncAuth", "Error saving data: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun syncWithDrive(drive: Drive): AuthSession = withContext(Dispatchers.IO) {
        val helper = DriveServiceHelper(drive)
        driveHelper = helper
        val session = sessionManager.getSession()
        
        Log.d("GutSyncAuth", "Starting syncWithDrive process on IO thread...")
        
        try {
            var folderId = Tasks.await(helper.searchFile("GutSync"))
            if (folderId == null) {
                folderId = Tasks.await(helper.createFolder("GutSync"))
            }

            var fileId = Tasks.await(helper.searchFileInFolder(folderId!!, "gutsync_data.json"))
            
            if (fileId == null) {
                val jsonStr = json.encodeToString(_appData.value)
                fileId = Tasks.await(helper.createFileInFolder(folderId, "gutsync_data.json", jsonStr))
            } else {
                val driveContent = Tasks.await(helper.readFile(fileId))
                _appData.value = json.decodeFromString<AppData>(driveContent)
            }

            // Successfully synced with Drive, delete local copy to ensure Drive is the only source
            if (localFile.exists()) {
                localFile.delete()
            }

            val updatedSession = session.copy(
                driveFolderId = folderId,
                driveFileId = fileId
            )
            sessionManager.saveSession(updatedSession)
            updatedSession
        } catch (e: Exception) {
            Log.e("GutSyncAuth", "FATAL ERROR during Drive Sync: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun addMeal(nutrients: NutrientData, imageBase64: String? = null, openRouterExplanation: String? = null) {
        val currentMeals = _appData.value.meals.toMutableList()
        currentMeals.add(MealLogEntry(nutrients, imageBase64 = imageBase64, openRouterExplanation = openRouterExplanation))
        _appData.value = _appData.value.copy(meals = currentMeals)
        saveData()
    }

    suspend fun updateChatSession(session: ChatSession) {
        val currentSessions = _appData.value.chatSessions.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == session.id }
        if (index != -1) {
            currentSessions[index] = session
        } else {
            currentSessions.add(session)
        }
        _appData.value = _appData.value.copy(chatSessions = currentSessions)
        saveData()
    }

    suspend fun updateProfile(profile: UserProfile) {
        _appData.value = _appData.value.copy(profile = profile)
        saveData()
    }
}
