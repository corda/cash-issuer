package com.r3.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.flows.UpdateAccountInfoRequest
import com.r3.cash.issuer.base.types.*
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.serialization.serialize
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * More thought needs to go into how this will work. Currently, each customer of the issuer is only allowed one bank
 * account. However, in the future they will be able to add multiple accounts. What does that API look like?
 */
@StartableByRPC
class UpdateAccountInfo(
        val issuer: Party,
        val accountName: String,
        val accountNumber: String,
        val sortCode: String
) : UpdateAccountInfoRequest() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val me = serviceHub.myInfo.legalIdentities.first()
        val accountInfo = AccountInfo(accountName, AccountNumber(accountNumber), SortCode(sortCode), owner = me.name)

        // Sign a hash of the account information object and send it to the issuer node.
        val hashOfSerialisedAccountInfo = accountInfo.serialize().hash.bytes
        val signature = serviceHub.keyManagementService.sign(hashOfSerialisedAccountInfo, me.owningKey)
        val session = initiateFlow(issuer)

        val response = session.sendAndReceive<UpdateAccountInfoResponse>(AccountInfoWithSignature(accountInfo, signature)).unwrap { it }
        // TODO: for demo in the console
        println(response)
    }

}