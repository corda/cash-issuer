package com.r3.cash.issuer.services

import com.r3.cash.issuer.base.types.AccountInfoWithSignature
import net.corda.core.identity.CordaX500Name

interface AccountService {
    fun updateAccount(member: CordaX500Name, accountInfo: AccountInfoWithSignature)
    fun getAccount(member: CordaX500Name): AccountInfoWithSignature?
    fun getAllAccounts(): Map<CordaX500Name, AccountInfoWithSignature>
}