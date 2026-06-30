package com.example.gutsync.data.storage

import android.content.Context
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

    suspend fun syncWithDrive(drive: Drive): AuthSession {
        val helper = DriveServiceHelper(drive)
        driveHelper = helper
        val session = sessionManager.getSession()
        
        var folderId = Tasks.await(helper.searchFile("GutSync"))
        if (folderId == null) {
            folderId = Tasks.await(helper.createFolder("GutSync"))
        }

        var fileId = Tasks.await(helper.searchFileInFolder(folderId!!, "gutsync_data.json"))
        
        if (fileId == null) {
            val jsonStr = Json.encodeToString(_appData.value)
            fileId = Tasks.await(helper.createFileInFolder(folderId, "gutsync_data.json", jsonStr))
        } else {
            val driveContent = Tasks.await(helper.readFile(fileId))
            _appData.value = Json.decodeFromString<AppData>(driveContent)
        }

        val updatedSession = session.copy(
            driveFolderId = folderId,
            driveFileId = fileId
        )
        sessionManager.saveSession(updatedSession)
        return updatedSession
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
