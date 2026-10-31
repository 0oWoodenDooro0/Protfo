## Problem Statement

Individual retail investors trading across both Taiwan and US stock markets frequently accumulate positions over time across irregular dates, quantities (including odd lots and fractional shares), and prices. Existing portfolio apps either require continuous network access and account logins, fail to properly compute weighted average cost basis across multiple lots, or present cluttered interfaces with unnecessary market noise, social feeds, and excessive visual clutter. Investors need a fast, privacy-focused, offline-first tool to manually record their stock transactions and accurately track their position sizes and cost basis with a clean, minimal Material 3 interface.

## Solution

Portfo is an offline-first Kotlin Multiplatform mobile application (targeting Android with Material 3 dynamic color theming) that serves as an immutable transaction ledger for Taiwan and US stock investments. The app separates Taiwan (TWD) and US (USD) markets into dedicated views, calculates position holdings and weighted average cost basis dynamically from raw transactions, and presents asset details through structured, high-density holding cards and history drill-downs without visual noise or redundant text.

## User Stories

1. As an investor, I want to switch between Taiwan (TWD) and US (USD) market tabs with a single tap, so that I can inspect my holdings in their respective native currencies without artificial exchange-rate conversions.
2. As an investor, I want to record a buy transaction for a Taiwan stock with stock code, name, unit price in TWD, integer shares, transaction date, and fees, so that my position and cost basis are captured accurately.
3. As an investor, I want to record a buy transaction for a US stock with ticker symbol, name, unit price in USD, fractional shares, transaction date, and broker fee, so that my fractional share purchases are accurately tracked.
4. As an investor, I want to view my aggregated holdings for each market displaying total shares, weighted average cost per share, and total cost basis, so that I know my exact investment position at a glance.
5. As an investor, I want to tap any stock holding card to open an asset detail view showing its complete historical transaction history, so that I can audit past purchases and sales chronologically.
6. As an investor, I want to record a sell transaction specifying price, shares, and fees, so that the system deducts the sold shares from my holding at the current weighted average cost and calculates the realized PnL.
7. As an investor, I want input validation to prevent recording a sell transaction whose share count exceeds my current holding, so that my portfolio does not end up with an impossible negative share balance.
8. As an investor, I want to edit any previously recorded transaction, so that I can correct accidental typos in price, shares, date, or fees.
9. As an investor, I want to delete an invalid transaction, so that my holdings, cost basis, and realized PnL automatically recalculate to reflect the corrected ledger.
10. As an investor, I want symbol and name inputs to offer autocomplete suggestions based on previously saved assets, so that I do not need to retype identical ticker information for recurring purchases.
11. As an investor, I want to see standard Material 3 icons for actions (such as add, edit, delete, back, filter) instead of redundant explanatory text labels, so that the user interface remains compact, uncluttered, and efficient.
12. As an investor, I want the application theme to dynamically adapt to my Android system color palette (Material You dynamic theming) and follow system light/dark mode settings, so that the app feels native and integrated with the operating system.
13. As an investor, I want all data stored locally in a robust SQLite database (Room KMP) without any network requests or telemetry, so that my financial transaction data remains private, fast, and accessible offline.

## Implementation Decisions

- **Architectural Pattern**: Unidirectional Data Flow (MVI/MVVM) built with Kotlin Multiplatform. The domain layer exposes reactive Kotlin `StateFlow` streams.
- **Database Layer**: `androidx.room` (Room KMP) with SQLite. Transactions table acts as the append-oriented immutable ledger.
- **Data Schema**:
  - `TransactionEntity`: `id` (Long PK), `market` (TW | US), `symbol` (String), `name` (String), `type` (BUY | SELL), `price` (Double), `shares` (Double), `fee` (Double), `tax` (Double), `transactedAt` (Long timestamp), `note` (String nullable).
  - `AssetEntity`: `symbol` (String), `market` (TW | US), `name` (String), `currency` (TWD | USD), `updatedAt` (Long) for rapid local autocompletion.
- **Computation Engine**: In-memory and DAO-level aggregation calculating Weighted Average Cost Basis:
  - For BUY: `newCostBasis = previousCostBasis + (price * shares) + fee`
  - `avgCost = newCostBasis / totalShares`
  - For SELL: `costOfSharesSold = avgCost * soldShares`
  - `realizedPnL += (price * soldShares) - fee - tax - costOfSharesSold`
  - `remainingCostBasis = previousCostBasis - costOfSharesSold`
- **UI Architecture**: Jetpack Compose / Compose Multiplatform using `androidx.compose.material3`.
  - Android 12+ dynamic color enabled via `dynamicLightColorScheme(context)` and `dynamicDarkColorScheme(context)`.
  - Strict absence of emojis across all UI copy, buttons, badges, and titles.
  - Text reduction: icon buttons for common actions (`Icons.Default.Add`, `Icons.Default.ArrowBack`, `Icons.Default.Edit`, `Icons.Default.Delete`, `Icons.Default.Check`, `Icons.Default.Close`).
  - Home layout: Top segmented button bar for market selection (TW / US), followed by market cost summary, followed by holding cards (Variant B layout: compact three-column metrics for shares, average cost, and cost basis).
  - Drill-down: Tapping a holding card opens the stock detail screen displaying chronological transaction records with buy/sell directional indicators.

## Testing Decisions

- **Primary Seam**: The highest-level non-UI seam is `PortfolioRepository` and `CalculatePortfolioUseCase`. Testing at this boundary validates transaction insertion, deletion, edge-case calculations (odd lots, fractional shares, partial sells, fee/tax deductions), and the resulting reactive `Holdings` state flow without UI dependencies.
- **Data Integrity Tests**: In-memory SQLite Room database tests verifying schema constraints, cascade behaviors, and ordering guarantees.
- **Calculation Unit Tests**:
  - Multiple sequential BUY lots verifying weighted average cost accuracy.
  - Partial SELL lots verifying accurate deduction of cost basis and correct calculation of realized PnL.
  - Full liquidation of a position returning holding balance to 0 and resetting cost basis to 0.
  - Fractional share precision preservation for US stocks (avoiding floating point rounding drift).
  - Validation rejections when sell quantity > holding quantity.

## Out of Scope

- Remote network synchronization, cloud backups, and third-party account linking.
- Real-time stock market quotes, live price scraping, and auto-updating market values.
- Unrealized PnL calculation (deferred until live quote integration is introduced).
- Stock split and dividend cash adjustment automation (can be entered manually as custom transactions).
- Cross-currency foreign exchange rate conversion between TWD and USD.

## Further Notes

- The design adheres to the domain model specified in `GLOSSARY.md` and architecture recorded in `docs/adr/0001-transaction-ledger-and-offline-room.md`.
- Prototype validation was performed using `prototype.html`.
