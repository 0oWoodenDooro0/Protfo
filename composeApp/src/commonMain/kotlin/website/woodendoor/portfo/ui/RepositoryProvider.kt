package website.woodendoor.portfo.ui

import androidx.compose.runtime.Composable
import website.woodendoor.portfo.domain.repository.PortfolioRepository

@Composable
expect fun rememberDefaultRepository(): PortfolioRepository
