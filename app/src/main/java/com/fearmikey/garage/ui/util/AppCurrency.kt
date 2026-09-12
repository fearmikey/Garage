package com.fearmikey.garage.ui.util

import java.util.Currency
import java.util.Locale

data class AppCurrency(
    val code: String,
    val symbol: String,
    val name: String,
) {
    val displayName: String get() = "$code — $name ($symbol)"

    companion object {
        val USD = AppCurrency("USD", "$", "US Dollar")
        val EUR = AppCurrency("EUR", "€", "Euro")
        val GBP = AppCurrency("GBP", "£", "British Pound")
        val CAD = AppCurrency("CAD", "CA$", "Canadian Dollar")
        val AUD = AppCurrency("AUD", "A$", "Australian Dollar")
        val JPY = AppCurrency("JPY", "¥", "Japanese Yen")
        val INR = AppCurrency("INR", "₹", "Indian Rupee")
        val CHF = AppCurrency("CHF", "CHF", "Swiss Franc")
        val BRL = AppCurrency("BRL", "R$", "Brazilian Real")
        val MXN = AppCurrency("MXN", "MX$", "Mexican Peso")
        val SEK = AppCurrency("SEK", "kr", "Swedish Krona")
        val NOK = AppCurrency("NOK", "kr", "Norwegian Krone")
        val DKK = AppCurrency("DKK", "kr", "Danish Krone")
        val PLN = AppCurrency("PLN", "zł", "Polish Złoty")
        val NZD = AppCurrency("NZD", "NZ$", "New Zealand Dollar")
        val SGD = AppCurrency("SGD", "S$", "Singapore Dollar")
        val HKD = AppCurrency("HKD", "HK$", "Hong Kong Dollar")
        val ZAR = AppCurrency("ZAR", "R", "South African Rand")
        val KRW = AppCurrency("KRW", "₩", "South Korean Won")
        val TRY = AppCurrency("TRY", "₺", "Turkish Lira")
        val PHP = AppCurrency("PHP", "₱", "Philippine Peso")
        val CZK = AppCurrency("CZK", "Kč", "Czech Koruna")
        val HUF = AppCurrency("HUF", "Ft", "Hungarian Forint")
        val ILS = AppCurrency("ILS", "₪", "Israeli Shekel")
        val THB = AppCurrency("THB", "฿", "Thai Baht")
        val AED = AppCurrency("AED", "AED", "UAE Dirham")
        val SAR = AppCurrency("SAR", "SAR", "Saudi Riyal")

        val SUPPORTED_CURRENCIES = listOf(
            USD, EUR, GBP, CAD, AUD, JPY, INR, CHF, BRL, MXN,
            SEK, NOK, DKK, PLN, NZD, SGD, HKD, ZAR, KRW, TRY,
            PHP, CZK, HUF, ILS, THB, AED, SAR
        )

        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return USD
            val match = SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
            if (match != null) return match
            return try {
                val javaCurrency = Currency.getInstance(code.uppercase())
                AppCurrency(
                    code = javaCurrency.currencyCode,
                    symbol = javaCurrency.getSymbol(Locale.US),
                    name = javaCurrency.getDisplayName(Locale.US) ?: javaCurrency.currencyCode
                )
            } catch (_: Exception) {
                USD
            }
        }
    }
}
