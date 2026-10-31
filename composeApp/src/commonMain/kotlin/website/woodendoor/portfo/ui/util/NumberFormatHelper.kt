package website.woodendoor.portfo.ui.util

import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object NumberFormatHelper {

    fun formatWithCommas(value: Long): String {
        val isNegative = value < 0
        val absStr = abs(value).toString()
        val sb = StringBuilder()
        for (i in absStr.indices) {
            if (i > 0 && (absStr.length - i) % 3 == 0) {
                sb.append(',')
            }
            sb.append(absStr[i])
        }
        return if (isNegative) "-$sb" else sb.toString()
    }

    fun formatCurrency(amount: Double, prefix: String = "NT$ ", decimals: Int = 0): String {
        return if (decimals == 0) {
            val rounded = round(amount).toLong()
            if (rounded < 0) {
                "-$prefix${formatWithCommas(abs(rounded))}"
            } else {
                "$prefix${formatWithCommas(rounded)}"
            }
        } else {
            val isNegative = amount < 0
            val absAmount = abs(amount)
            val formatted = formatDecimal(absAmount, decimals)
            val isZero = formatted == "0.${"0".repeat(decimals)}"
            if (isNegative && !isZero) {
                "-$prefix$formatted"
            } else {
                "$prefix$formatted"
            }
        }
    }

    fun formatDecimal(
        value: Double,
        decimals: Int = 2,
        stripTrailingZeroDecimals: Boolean = false
    ): String {
        if (decimals == 0) {
            val rounded = round(abs(value)).toLong()
            val isNegative = value < 0
            val formatted = formatWithCommas(rounded)
            return if (isNegative && rounded != 0L) "-$formatted" else formatted
        }
        val factor = when (decimals) {
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            4 -> 10000.0
            else -> 100.0
        }
        val isNegative = value < 0
        val absVal = abs(value)
        val rounded = round(absVal * factor) / factor
        val parts = rounded.toString().split('.')
        val intPart = formatWithCommas(parts[0].toLongOrNull() ?: 0L)
        val fracPart = if (parts.size > 1) parts[1].padEnd(decimals, '0').take(decimals) else "0".repeat(decimals)

        if (stripTrailingZeroDecimals && fracPart.all { it == '0' }) {
            val isZero = (parts[0].toLongOrNull() ?: 0L) == 0L
            return if (isNegative && !isZero) "-$intPart" else intPart
        }

        val formatted = "$intPart.$fracPart"
        val isZero = (parts[0].toLongOrNull() ?: 0L) == 0L && fracPart.all { it == '0' }
        return if (isNegative && !isZero) "-$formatted" else formatted
    }

    /**
     * Formats cost or price. If the fractional part is .00 (or all zeros),
     * automatically strips the decimal point and trailing zero digits.
     */
    fun formatCost(value: Double, decimals: Int = 2): String {
        return formatDecimal(value, decimals = decimals, stripTrailingZeroDecimals = true)
    }

    fun formatShares(shares: Double, isIntegerMarket: Boolean = true): String {
        return if (isIntegerMarket) {
            "${formatWithCommas(shares.toLong())} 股"
        } else {
            val rounded2 = round(shares * 100.0) / 100.0
            if (abs(shares - rounded2) > 1e-4) {
                val str4 = formatDecimal(shares, 4).trimEnd('0')
                "$str4 股"
            } else {
                "${formatDecimal(shares, 2)} 股"
            }
        }
    }

    @Suppress("DEPRECATION")
    fun formatDate(epochMillis: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val y = localDate.year.toString().padStart(4, '0')
            val m = localDate.monthNumber.toString().padStart(2, '0')
            val d = localDate.dayOfMonth.toString().padStart(2, '0')
            "$y-$m-$d"
        } catch (_: Exception) {
            "2026-10-31"
        }
    }

    fun parseDateToEpochMillis(dateStr: String): Long {
        return try {
            val parts = dateStr.trim().split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                val date = LocalDate(year, month, day)
                date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            } else {
                Clock.System.now().toEpochMilliseconds()
            }
        } catch (_: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
