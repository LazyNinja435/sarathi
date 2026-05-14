package com.sarathi.app.model

sealed class ModelStatus {
    data object Missing : ModelStatus()
    data object Loading : ModelStatus()
    data class Installed(val path: String) : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}
