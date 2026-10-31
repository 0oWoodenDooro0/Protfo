package website.woodendoor.portfo.ui.util

import kotlin.math.abs
import kotlin.math.round

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

    fun formatCurrency(amount: Double, prefix: String = "NT$ "): String {
        val rounded = round(amount).toLong()
        return "$prefix${formatWithCommas(rounded)}"
    }

    fun formatDecimal(value: Double, decimals: Int = 2): String {
        val factor = when (decimals) {
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            else -> 100.0
        }
        val rounded = round(value * factor) / factor
        val parts = rounded.toString().split('.')
        val intPart = formatWithCommas(parts[0].toLongOrNull() ?: 0L)
        val fracPart = if (parts.size > 1) parts[1].padEnd(decimals, '0').take(decimals) else "0".repeat(decimals)
        return "$intPart.$fracPart"
    }

    fun formatShares(shares: Double, isIntegerMarket: Boolean = true): String {
        return if (isIntegerMarket || shares % 1.0 == 0.0) {
            "${formatWithCommas(shares.toLong())} 股"
        } else {
            "${formatDecimal(shares, 3)} 股"
        }
    }
}
