package com.example.gutsync.data.auth

import kotlinx.serialization.Serializable

enum class AccountType {
    OFFLINE,
    GOOGLE
}

@Serializable
data class AuthSession(
    val isLoggedIn: Boolean = false,
    val accountType: AccountType = AccountType.OFFLINE,
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val driveFolderId: String? = null,
    val driveFileId: String? = null,
    val offlineUserId: String? = null
) {
    val isDriveConnected: Boolean
        get() = driveFolderId != null && driveFileId != null
}
