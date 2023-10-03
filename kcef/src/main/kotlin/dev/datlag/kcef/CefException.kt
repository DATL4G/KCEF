package dev.datlag.kcef

sealed class CefException(override val message: String) : Exception(message) {
    data object Startup : CefException("JCef did not initialize correctly!")
    data class UnsupportedPlatform(
        val os: String?,
        val arch: String?
    ) : CefException("Can not detect your current platform. Is it supported? [$os, $arch]")

    data object InstallationDirectory : CefException("Could not create installation directory")
    data object InstallationLock : CefException("Could not create install.lock to complete installation")

    data class UnsupportedPlatformPackage(
        val os: String?,
        val arch: String?
    ) : CefException("No available package found for your platform [$os, $arch]")

    data object DownloadTempFile : CefException("Could not create temp file to download jcef package")

    data object Download : CefException("Could not download jcef package")

    data object BadArchive : CefException("The provided archive contains a bad (malicious) file")

    data object NotInitialized : CefException("Cef was not initialized.")

    data object Disposed : CefException("Cef is disposed.")

    data object ApplicationRestartRequired : CefException("Application needs to restart.")

    data class Error(override val cause: Throwable?) : CefException("Got error: ${cause?.message}")
}