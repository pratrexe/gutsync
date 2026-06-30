package com.example.gutsync.data.storage

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DriveServiceHelper(private val mDriveService: Drive) {
    private val mExecutor: Executor = Executors.newSingleThreadExecutor()

    /**
     * Creates a folder in the user's My Drive folder and returns its ID.
     */
    fun createFolder(folderName: String): Task<String> {
        return Tasks.call(mExecutor) {
            val metadata = File()
                .setName(folderName)
                .setMimeType("application/vnd.google-apps.folder")

            val googleFile = mDriveService.files().create(metadata).execute()
                ?: throw Exception("Null folder when requesting folder creation.")

            googleFile.id
        }
    }

    /**
     * Creates a text file in a specific folder and returns its file ID.
     */
    fun createFileInFolder(folderId: String, fileName: String, content: String): Task<String> {
        return Tasks.call(mExecutor) {
            val metadata = File()
                .setName(fileName)
                .setMimeType("application/json")
                .setParents(listOf(folderId))

            val contentStream = ByteArrayContent.fromString("application/json", content)
            val googleFile = mDriveService.files().create(metadata, contentStream).execute()
                ?: throw Exception("Null file when requesting file creation in folder.")

            googleFile.id
        }
    }

    /**
     * Searches for a file by name inside a specific folder and returns its ID.
     */
    fun searchFileInFolder(folderId: String, fileName: String): Task<String?> {
        return Tasks.call(mExecutor) {
            val result: FileList = mDriveService.files().list()
                .setQ("name = '$fileName' and '$folderId' in parents and trashed = false")
                .setSpaces("drive")
                .execute()

            if (result.files.isNotEmpty()) {
                result.files[0].id
            } else {
                null
            }
        }
    }

    /**
     * Updates a text file in the user's My Drive folder.
     */
    fun updateFile(fileId: String, content: String): Task<Void> {
        return Tasks.call(mExecutor) {
            val contentStream = ByteArrayContent.fromString("application/json", content)
            mDriveService.files().update(fileId, null, contentStream).execute()
            null
        }
    }

    /**
     * Returns the content of a text file in the user's My Drive folder.
     */
    fun readFile(fileId: String): Task<String> {
        return Tasks.call(mExecutor) {
            mDriveService.files().get(fileId).executeMediaAsInputStream().bufferedReader().use { it.readText() }
        }
    }

    /**
     * Searches for a file by name and returns its ID.
     */
    fun searchFile(fileName: String): Task<String?> {
        return Tasks.call(mExecutor) {
            val result: FileList = mDriveService.files().list()
                .setQ("name = '$fileName' and trashed = false")
                .setSpaces("drive")
                .execute()
            
            if (result.files.isNotEmpty()) {
                result.files[0].id
            } else {
                null
            }
        }
    }
}
