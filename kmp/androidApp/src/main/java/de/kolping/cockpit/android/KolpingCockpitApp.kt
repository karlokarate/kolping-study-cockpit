package de.kolping.cockpit.android

import android.app.Application
import de.kolping.cockpit.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class KolpingCockpitApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@KolpingCockpitApp)
            modules(appModule)
        }
    }
}
