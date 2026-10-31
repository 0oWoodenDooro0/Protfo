package website.woodendoor.portfo.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createPortfoDatabase(
    context: Context,
    useBundledDriver: Boolean = false
): PortfoDatabase {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("portfo.db")
    val builder = Room.databaseBuilder<PortfoDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setQueryCoroutineContext(Dispatchers.IO)

    if (useBundledDriver) {
        builder.setDriver(BundledSQLiteDriver())
    }
    return builder.build()
}

fun createInMemoryPortfoDatabase(context: Context): PortfoDatabase {
    return Room.inMemoryDatabaseBuilder<PortfoDatabase>(
        context = context
    )
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
