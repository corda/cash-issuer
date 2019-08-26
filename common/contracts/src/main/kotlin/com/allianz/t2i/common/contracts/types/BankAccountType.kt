package com.allianz.t2i.common.contracts.types

import net.corda.core.serialization.CordaSerializable

/**
 * The issuer operates a multitude of bank accounts, most of them are collateral accounts. However, not all of them
 * are. As per electronic money regulations in the EU, the issuer is entitled to overnight interest on relevant funds
 * balances. Therefore, the issuer will need to sweep interest income into a separate account. The issuer, when
 * permitted, may from time to time, sweep cash in respect of fees from any of the collateral accounts into its
 * operational accounts. Bank account type must be taken into account when inspecting transactions between accounts
 * operated by the issuer. For example: an outflow from a collateral account and a corresponding in flow into an
 * issuer account is likely because some fees were due from a customer. On the other hand, an out flow from a collateral
 * account with a corresponding in flow for the same amount into another collateral account will be due to liquidity
 * management between the various nostro accounts which the issuer operates.
 */
@CordaSerializable
enum class BankAccountType {
    COLLATERAL,
    ISSUER
}