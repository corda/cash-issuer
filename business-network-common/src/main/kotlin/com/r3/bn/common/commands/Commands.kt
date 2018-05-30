package com.r3.bn.common.commands

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TypeOnlyCommandData

/**
 * A common interface for all commands related to business network administration.
 */
interface BusinessNetworkAdministrationCommand : CommandData

/**
 * For use when requesting to join a business network.
 */
class Create : BusinessNetworkAdministrationCommand, TypeOnlyCommandData()

/**
 * For use when updating a membership record.
 */
class Update : BusinessNetworkAdministrationCommand