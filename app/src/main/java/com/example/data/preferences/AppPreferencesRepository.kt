package com.example.data.preferences

data class AppPrefsState(
    val lang: String = "en",
    val brand: String = "KaamWala",
    val city: String = "Lahore",
    val whatsapp: String = "923001234567",
    val payAccount: String = "JazzCash / Easypaisa: 0300-1234567",
    val leadFee: Int = 50,
    val commissionPercent: Int = 10,
    val staffPin: String = "1234",
    val liveCustomerMode: Boolean = false,
    val customerCredit: Int = 0,
    val walletBalance: Int = 1000,
    val appRevenue: Int = 50,
    val workerOnline: Boolean = true,
    val workerStrikes: Int = 0,
    val usedFirstPromo: Boolean = false,
    val activeWorkerName: String = "Ustad Bilal Ahmed",
    val isSeeded: Boolean = false
)
