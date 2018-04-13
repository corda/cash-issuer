package com.r3.corda.finance.cash.issuer.daemon

import net.corda.core.toFuture
import net.corda.core.utilities.getOrThrow
import rx.Observable
import rx.schedulers.Schedulers

// Helper to convert an observable that emits one event to a future, with the work being performed on an IO thread.
fun <T : Any> Observable<T>.getOrThrow() = observeOn(Schedulers.io()).doOnError { println(it.message) }.toFuture().getOrThrow()!!

//@StartableByService
//class Test : FlowLogic<List<Pair<Long, String>>>() {
//    @Suspendable
//    override fun call(): List<Pair<Long, String>> {
//        val query = builder {
//            val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
//            val nostroTransactionCriteria = QueryCriteria.VaultCustomQueryCriteria(
//                    // Return transactions with the highest timestamp grouped by "accountId".
//                    NostroTransactionSchemaV1.PersistentNostroTransactionState::createdAt.max(
//                            groupByColumns = listOf(NostroTransactionSchemaV1.PersistentNostroTransactionState::accountId)
//                    )
//            )
//            generalCriteria.and(nostroTransactionCriteria)
//        }
//        val results = serviceHub.vaultService.queryBy<NostroTransactionState>(query).otherResults
//
//        // When returning grouped results, the vault only returns an untyped list
//        // containing the result of the aggregate function and the column which was
//        // used to group the results. Developers need to manually
//        return Lists.partition(results, 2).map {
//            Pair(it[1] as Long, it[0] as String)
//        }
//    }
//}