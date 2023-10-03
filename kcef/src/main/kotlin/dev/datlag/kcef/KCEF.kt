package dev.datlag.kcef

import dev.datlag.kcef.common.existsSafely
import dev.datlag.kcef.common.suspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cef.CefApp
import org.cef.CefClient
import java.io.File
import kotlin.properties.Delegates

/**
 * Class used to initialize the JCef environment.
 *
 * Create a new [CefClient] after initialization easily.
 *
 * Dispose the JCef environment if you don't need it anymore.
 */
data object KCEF {

    private val state: MutableStateFlow<State> = MutableStateFlow(State.New)
    private var cefApp by Delegates.notNull<CefApp>()

    /**
     * Download, install and initialize CEF on the client.
     *
     * @param builder builder method to create a [KCEFBuilder] to use
     * @param onError an optional listener for errors
     * @param onRestartRequired an optional listener to be notified when the application needs a restart,
     * may happen on some platforms if CEF couldn't be initialized after downloading and installing.
     * @throws CefException.Disposed if you called [dispose] or [disposeBlocking] previously
     */
    @JvmStatic
    @JvmOverloads
    @Throws(CefException.Disposed::class)
    suspend fun init(
        builder: KCEFBuilder.() -> Unit,
        onError: InitError = InitError {  },
        onRestartRequired: InitRestartRequired = InitRestartRequired {  }
    ) = init(
        builder = KCEFBuilder().apply(builder),
        onError = onError,
        onRestartRequired = onRestartRequired
    )

    /**
     * Blocking equivalent of [init]
     *
     * @see init
     */
    @JvmStatic
    @JvmOverloads
    @Throws(CefException.Disposed::class)
    fun initBlocking(
        builder: KCEFBuilder.() -> Unit,
        onError: InitError = InitError {  },
        onRestartRequired: InitRestartRequired = InitRestartRequired {  }
    ) = runBlocking {
        init(builder, onError, onRestartRequired)
    }

    /**
     * Download, install and initialize CEF on the client.
     *
     * @param builder the [KCEFBuilder] to use
     * @param onError an optional listener for errors
     * @param onRestartRequired an optional listener to be notified when the application needs a restart,
     * may happen on some platforms if CEF couldn't be initialized after downloading and installing.
     * @throws CefException.Disposed if you called [dispose] or [disposeBlocking] previously
     */
    @JvmStatic
    @JvmOverloads
    @Throws(CefException.Disposed::class)
    suspend fun init(
        builder: KCEFBuilder,
        onError: InitError = InitError {  },
        onRestartRequired: InitRestartRequired = InitRestartRequired {  }
    ) {
        val currentBuilder = when (state.value) {
            State.Disposed -> throw CefException.Disposed
            State.Initializing, State.Initialized -> null
            State.New, is State.Error -> {
                state.emit(State.Initializing)
                builder
            }
        } ?: return

        val installOk = File(builder.installDir, "install.lock").existsSafely()

        if (installOk) {
            val result = suspendCatching {
                currentBuilder.build()
            }
            setInitResult(result)
            result.exceptionOrNull()?.let(onError::invoke)
        } else {
            val installResult = suspendCatching {
                builder.install()
            }
            installResult.exceptionOrNull()?.let {
                setInitResult(Result.failure(it))
                onError(it)
            }

            val result = suspendCatching {
                builder.build()
            }

            setInitResult(result)
            if (result.isFailure) {
                result.exceptionOrNull()?.let(onError::invoke)
                setInitResult(Result.failure(CefException.ApplicationRestartRequired))
                onRestartRequired()
            }
        }
    }

    /**
     * Blocking equivalent of [init]
     *
     * @see init
     */
    @JvmStatic
    @JvmOverloads
    @Throws(CefException.Disposed::class)
    fun initBlocking(
        builder: KCEFBuilder,
        onError: InitError = InitError {  },
        onRestartRequired: InitRestartRequired = InitRestartRequired {  }
    ) = runBlocking {
        init(builder, onError, onRestartRequired)
    }

    /**
     * Create a new CefClient after CEF has been initialized.
     *
     * Waits for initialization if it isn't finished yet.
     *
     * @see init to initialize CEF
     * @throws CefException.NotInitialized if the [init] method have not been called.
     * @throws CefException.Disposed if you called [dispose] or [disposeBlocking] previously
     * @throws CefException.Error if any other error occurred during initialization
     * @return [CefClient] after initialization
     */
    @JvmStatic
    @Throws(
        CefException.NotInitialized::class,
        CefException.Disposed::class,
        CefException.Error::class
    )
    suspend fun newClient(): CefClient {
        return when (state.value) {
            State.New -> throw CefException.NotInitialized
            State.Disposed -> throw CefException.Disposed
            is State.Error -> throw CefException.Error((state.value as? State.Error)?.exception)
            State.Initialized -> cefApp.createClient()
            State.Initializing -> {
                state.first { it != State.Initializing }

                return newClient()
            }
        }
    }

    /**
     * Blocking equivalent of [newClient]
     *
     * @see newClient to initialize CEF
     */
    @JvmStatic
    @Throws(
        CefException.NotInitialized::class,
        CefException.Disposed::class,
        CefException.Error::class
    )
    fun newClientBlocking(): CefClient = runBlocking {
        newClient()
    }

    /**
     * Create a new CefClient after CEF has been initialized.
     *
     * Waits for initialization if it isn't finished yet.
     *
     * @see init to initialize CEF
     * @param onError an optional listener for any error occurred during initialization
     * @return [CefClient] after initialization or null if any error occurred
     */
    @JvmStatic
    @JvmOverloads
    suspend fun newClientOrNull(onError: NewClientOrNullError = NewClientOrNullError {  }): CefClient? {
        return when (state.value) {
            State.New -> {
                onError(CefException.NotInitialized)
                null
            }
            State.Disposed -> {
                onError(CefException.Disposed)
                null
            }
            is State.Error -> {
                onError(CefException.Error((state.value as? State.Error)?.exception))
                null
            }
            State.Initialized -> cefApp.createClient()
            State.Initializing -> {
                state.first { it != State.Initializing }

                return newClientOrNull(onError)
            }
        }
    }

    /**
     * Blocking equivalent of [newClientOrNull]
     *
     * @see newClientOrNull to initialize CEF
     */
    @JvmStatic
    @JvmOverloads
    fun newClientOrNullBlocking(onError: NewClientOrNullError = NewClientOrNullError {  }): CefClient? = runBlocking {
        newClientOrNull(onError)
    }

    /**
     * Dispose the [CefApp] instance if it is not needed anymore.
     * For example on exiting the application.
     *
     * Waits for initialization if it isn't finished yet
     */
    @JvmStatic
    suspend fun dispose() {
        when (state.value) {
            State.New, State.Disposed, is State.Error -> return
            State.Initializing -> {
                state.first { it != State.Initializing }

                return dispose()
            }
            State.Initialized -> {
                state.emit(State.Disposed)
                cefApp.dispose()
            }
        }
    }

    /**
     * Blocking equivalent of [dispose]
     *
     * @see dispose to initialize CEF
     */
    @JvmStatic
    fun disposeBlocking() = runBlocking {
        dispose()
    }

    private fun setInitResult(result: Result<CefApp>): Boolean {
        val nextState = if (result.isSuccess) {
            cefApp = result.getOrThrow()
            State.Initialized
        } else {
            State.Error(result.exceptionOrNull())
        }

        return state.compareAndSet(State.Initializing, nextState)
    }

    private sealed class State {
        data object New : State()
        data object Initializing : State()
        data object Initialized : State()
        data class Error(val exception: Throwable?) : State()
        data object Disposed : State()
    }

    fun interface InitError {
        operator fun invoke(throwable: Throwable?)
    }

    fun interface InitRestartRequired {
        operator fun invoke()
    }

    fun interface NewClientOrNullError {
        operator fun invoke(throwable: Throwable?)
    }
}