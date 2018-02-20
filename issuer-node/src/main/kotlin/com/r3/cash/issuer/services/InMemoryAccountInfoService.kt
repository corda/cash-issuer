package com.r3.cash.issuer.services

import com.r3.cash.issuer.base.types.AccountInfoWithSignature
import com.r3.cash.issuer.base.types.RecordStatus
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class InMemoryAccountInfoService(val services: AppServiceHub) : SingletonSerializeAsToken(), AccountService {

    private val accountInfoMap = hashMapOf<CordaX500Name, AccountInfoWithSignature>()

    override fun updateAccount(member: CordaX500Name, accountInfo: AccountInfoWithSignature) {
        val memberService = services.cordaService(InMemoryMembershipService::class.java)
        val record = memberService.getRecord(member) ?: throw IllegalArgumentException("Couldn't find record for $member.")

        if (record.status in listOf(RecordStatus.REQUESTED, RecordStatus.SUSPENDED, RecordStatus.REJECTED)) {
            throw IllegalArgumentException("Can only add account information for APPROVED members.")
        }

        accountInfoMap.put(member, accountInfo)
    }

    override fun getAccount(member: CordaX500Name): AccountInfoWithSignature? = accountInfoMap[member]

    override fun getAllAccounts(): Map<CordaX500Name, AccountInfoWithSignature> = accountInfoMap.toMap()

}