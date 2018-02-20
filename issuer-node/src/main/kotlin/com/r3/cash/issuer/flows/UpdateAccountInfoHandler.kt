package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.flows.UpdateAccountInfoRequest
import com.r3.cash.issuer.base.flows.UpdateAccountInfoRequestHandler
import com.r3.cash.issuer.base.types.AccountInfoWithSignature
import com.r3.cash.issuer.base.types.UpdateAccountInfoResponse
import com.r3.cash.issuer.services.InMemoryAccountInfoService
import net.corda.core.crypto.Crypto
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.serialization.serialize
import net.corda.core.utilities.unwrap

@InitiatedBy(UpdateAccountInfoRequest::class)
class UpdateAccountInfoHandler(otherSession: FlowSession) : UpdateAccountInfoRequestHandler(otherSession) {

    @Suspendable
    override fun call() {
        val accountInfoWithSignature = otherSession.receive<AccountInfoWithSignature>().unwrap { it }

        require(accountInfoWithSignature.accountInfo.owner == otherSession.counterparty.name) {
            otherSession.send(UpdateAccountInfoResponse.Failure("Owner X500Name doesn't match sending node's X500Name."))
        }

        // Verify signature.
        val hashOfAccountInformation = accountInfoWithSignature.accountInfo.serialize().hash
        val pubKey = otherSession.counterparty.owningKey
        val sig = accountInfoWithSignature.signature.bytes
        val data = hashOfAccountInformation.bytes

        // Bail if the signature doesn't check out.
        if (Crypto.doVerify(pubKey, sig, data).not()) {
            otherSession.send(UpdateAccountInfoResponse.Failure("Signature over account info is invalid."))
        }

        val accountInfoService = serviceHub.cordaService(InMemoryAccountInfoService::class.java)
        accountInfoService.updateAccount(otherSession.counterparty.name, accountInfoWithSignature)
        otherSession.send(UpdateAccountInfoResponse.Success())
    }
}