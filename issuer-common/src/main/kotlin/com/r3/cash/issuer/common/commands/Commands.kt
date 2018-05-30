package com.r3.cash.issuer.common.commands

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TypeOnlyCommandData

/**
 * A common interface for all commands related to administration around cash issuance.
 */
interface CashIssuerAdministrationCommand : CommandData

/**
 * For use when adding a new bank account record.
 */
class Create : CashIssuerAdministrationCommand, TypeOnlyCommandData()

/**
 * For use when updating a bank account record.
 */
class Update : CashIssuerAdministrationCommand, TypeOnlyCommandData()

/**
 * For use when deleting a membership record.
 */
class Delete : CashIssuerAdministrationCommand, TypeOnlyCommandData()