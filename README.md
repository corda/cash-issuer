![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Cash Issuer

**WARNING: DO NOT USE IN PRODUCTION!**

**WARNING:** To run this code, you need to use version 4.0-SNAPSHOT of
Corda or greater. This code also only works with UK bank accounts that
provide open banking APIs.

This repo contains a reference implementation of the Corda cash issuer as
described in the [accompanying design document](design/design.md).

The documentation is incomplete and the code is more instructive than
anything else. If you do want to use this code and get stuck then e-mail
roger.willis@r3.com.

The repo is split into two modules:

1. **cash** - a placeholder for the core finance state and contract types.
   For now there is just a placeholder cash contract.
2. **cash-issuer** - the cash issuer codebase.

The cash-issuer is split into a number of sub modules:

1. **client** - code which should be run by participants in a cash business
   network.
2. **common** - code which is shared by the cash issuer and users of cash states
   issued by the cash issuer. E.g. abstract flow initiator definitions
   and types.
3. **daemon** - a process which polls bank APIs for new transactions,
   transforms the data into a common format and sends it to the issuer
   node for processing
4. **service** - the cash issuer node. Contains folows for processing data
   provided by the daemon as well as flows for issuing and redeeming cash
   states
5. **service-ui** - a basic JavaFx app that provides a view on the cash issuer
   node.

## Requirements

1. Three bank accounts. One for the Issuer, one for PartyA and one for
   partyB.
2. The bank holding the Issuer's bank account needs to offer a
   public API which allows clients to get account information, balance
   information and transaction information in real time.
3. You will need a working API key for the bank's API.
4. A special SNAPSHOT version of Corda (Ask Roger for more information).

## How to use this code

Add your own bank API clients:

1. The daemon is extensible. Support for any bank HTTP API can be added by
   sub-classing `OpenBankingApiClient` and providing an interface definition
   for the API that can be used by Retrofit. Look at the [Monzo](cash-issuer/daemon/src/main/kotlin/com/r3/corda/finance/cash/issuer/daemon/clients/Monzo.kt)
   and [Starling](cash-issuer/daemon/src/main/kotlin/com/r3/corda/finance/cash/issuer/daemon/clients/Starling.kt)
   implementations as examples.
2. You will notice that the Starling and Monzo implementations differ a
   little, this is due to the different API interfaces offered.
3. You will need to add a config file to the resources folder, For,
   example to add support for "Foo Bank", add a "FooBankClient" class that
   sub-classes `OpenBankingApiClient` and add a `foobank.conf` config file
   (omit 'Client' from the config file name). Config files contain three
   key/value pairs:

   ```
   apiBaseUrl="[URL HERE]"
   apiVersion=""
   apiAccessToken="[ADD ACCESS TOKEN HERE"
   ```

If you have your own Starling or Monzo account you'd like to use, then
no additional work is required.

## Getting started

Start the corda nodes and issuer daemon:

1. Assuming all the API clients you need are implemented and a working
   config is present in the `resources` directory, then you are good to
   go!
2. From the root of this repo run `./gradlew clean deployNodes`. The
   deployNodes script will build `Notary` `Issuer`, `PartyA` and `PartyB`
   nodes.
3. Navigate to the node directories `cd build/nodes`.
4. Run the nodes `./runnodes`.
5. Wait for all the nodes to start up.
6. Start the issuer daemon (See "Starting the issuer daemon" below).
7. Start the issuer `service-ui`. Run via the Green Arrow next to the
   `main` function in `com/r3/corda/finance/cash/issuer/Main.kt`. The app
   is defaulted to connect to the Issuer node on port 10006. This can be
   changed in `Main.kt` if required.

At this point all the required processes are up and running. Next, you can
perform a demo run of an issuance:

8. From `PartyA` add a new bank account via the node shell: `flow start Add bankAccount: { accountId: 12345, accountName: Rogers Account, accountNumber: { sortCode: “XXXXXX”, accountNumber: YYYYYYYY, type: uk }, currency: GBP }`
   replacing `XXXXXX` and `YYYYYYYY` with your sort code and account number.
   This is the bank account that you will make a payment from, to the issuer's
   account.
9. Next, we need to send the bank account you have just added, to the
   issuer node. First, we need to know the linear ID of the bank account
   state which has just been added: `run vaultQuery contractStateType: com.r3.corda.finance.cash.issuer.common.states.BankAccountState`.
   You should see the linear ID in the data structure which is output to the shell.
   Send the account to the issuer with `start Send issuer: Issuer, linearId: LINEAR_ID`.
10. You should see the issuer's UI update with new bank account information.
    Note: the issuer's account should already be added.
11. From the issuer daemon shell type `start`. The daemon should start
    polling for new transactions.
12. Make a payment (for a small amount!!) from `PartyA`s bank account to
    the `Issuer`s bank account. Soon after the payment has been made, the
    daemon should pick up the transaction information and the Issuer UI
    should update in the "nostro transactions" pane and the "node transactions"
    pane.
13. Assuming the correct details for the bank account used by PartyA were
    added and successfully sent to the issuer, then the issuance record in
    the node transaction tab should be marked as complete.
14. Run `run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State`
    from PartyA to inspect the amount of cash issued. It should be for
    the same amount of the payment sent to the issuer's account.

## Starting the issuer daemon

1. Start the daemon via the main method in `Main.kt`. The daemon should
   start and present you with a simple command line interface. The daemon
   requires a number of command line parameters. The main ones to know are:
   ```
   host-port    the host name and port of the code node to connect to
   rpcUser      the RPC username for the corda node
   rpcPass      the RPC password for the corda node
   ```
   All three of the above arguments are required. As such, note that if
   no corda node is available to connect to on the specified hostname and
   port, then the daemon will not start.
2. When the daemon starts up, it requests bank account information for all
   the supplied API interfaces. It then uploads the account information to
   the issuer node, via RPC. Note: if the daemon is connected to a corda
   node which does not have the required flows, then the daemon and the corda
   node in question will throw an exception. Make sure that the daemon
   only connects to issuers nodes as defined in the `service` module! Once
   account information has been added. It requests the balance information
   for each of the added accounts. Lastly, it presents a basic shell.
   Current commands are:
   ```
   start        starts polling the apis for new transactions
                with a 5 second interval
   stop         stop polling
   help         show help
   quit         exit the daemon
   ```

