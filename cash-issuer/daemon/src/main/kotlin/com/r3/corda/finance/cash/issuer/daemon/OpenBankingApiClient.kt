package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

typealias BankAccountId = String

data class ApiConfig(val apiBaseUrl: String, val apiAccessToken: String, val accounts: List<Pair<String, String>>)

// TODO: Add a feature to this class that allows sub-classes to filter accounts based on the whitelist (in the config file).
abstract class OpenBankingApiClient(val configName: String) {
    abstract val api: Any
    abstract val accounts: List<BankAccount>

    protected val apiConfig: ApiConfig by lazy {
        val config: Config = ConfigFactory.parseResources("$configName.conf")
        val apiBaseUrl: String = config.getString("apiBaseUrl") + config.getString("apiVersion")
        val apiAccessToken: String = config.getString("apiAccessToken")
        val accounts = config.getConfigList("accounts").map { Pair(it.getString("number"), it.getString("type")) }
        if (accounts.isEmpty()) {
            // TODO: Currently one must specify accounts but they are not yet used.
            throw IllegalStateException("ERROR: No bank accounts have been specified for $configName.")
        }
        ApiConfig(apiBaseUrl, apiAccessToken, accounts)
    }
}


