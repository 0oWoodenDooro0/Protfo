package website.woodendoor.portfo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import website.woodendoor.portfo.data.database.createPortfoDatabase
import website.woodendoor.portfo.data.repository.PortfolioRepositoryImpl
import website.woodendoor.portfo.domain.repository.PortfolioRepository

@Composable
actual fun rememberDefaultRepository(): PortfolioRepository {
    val context = LocalContext.current
    return remember(context) {
        val db = createPortfoDatabase(context)
        PortfolioRepositoryImpl(
            transactionDao = db.transactionDao(),
            assetDao = db.assetDao()
        )
    }
}
