package cloud.cyberverse.asterion

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import cloud.cyberverse.asterion.data.download.AsterionWorkerFactory
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import cloud.cyberverse.asterion.di.downloadModule
import cloud.cyberverse.asterion.di.networkModule
import cloud.cyberverse.asterion.di.viewModelModule
import com.clerk.api.Clerk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AsterionApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(this, BuildConfig.CLERK_PUBLISHABLE_KEY)
        val koinApp = startKoin {
            androidContext(this@AsterionApp)
            modules(networkModule, viewModelModule, downloadModule)
        }
        CoroutineScope(Dispatchers.IO).launch {
            koinApp.koin.get<DownloadIndexStore>().load()
        }
        // The manifest removes WorkManager's default App Startup initializer (it builds Workers
        // via reflection and can't reach Koin-provided dependencies), so it must be started
        // explicitly here with the Koin-aware factory instead.
        WorkManager.initialize(this, workManagerConfiguration)
    }

    // WorkManager builds Workers via reflection by default and can't reach Koin-provided
    // dependencies that way - this routes it through AsterionWorkerFactory instead.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(AsterionWorkerFactory())
            .build()
}
