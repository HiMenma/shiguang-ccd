package app.shiguang.ccd

import android.app.Application
import android.content.Context
import app.shiguang.ccd.data.GalleryRepository
import app.shiguang.ccd.data.OriginalStore
import app.shiguang.ccd.data.SettingsStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppContainer(context: Context) {
    val gallery = GalleryRepository(context.applicationContext)
    val settings = SettingsStore(context.applicationContext)
    val originals = OriginalStore(context.applicationContext)
    val io: ExecutorService = Executors.newSingleThreadExecutor()
}

class ShiguangApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
