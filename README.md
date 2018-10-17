![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Cash Issuer

## WARNING:

**!!! DO NOT USE IN PRODUCTION !!!**

**!! DO NOT USE WITH REAL CUSTOMERS OR REAL MONEY WITHOUT APPROPRIATE LICENSES AND REGULATORY ENGAGEMENT !!**

**!! THIS CODE IS INTENDED TO BE USED WITH EPHEMERAL CORDA NETWORKS FOR DEMO 
PUROSES ONLY !!**

This code use a nightly snapshot (4.0-SNAPSHOT) release of Corda 
obtained from the R3 artifactory repository. There is a chance that a particular
build could be unstable!

This code works with Monzo and Starling bank accounts. For those that do not have
Monzo or Starling bank accounts you can use the `MockMonzo` client to generate
fake transaction data.

This repo contains an example of how to implement a cash issuer/cash tokenizer as
described in the [accompanying design document](design/design.md).

The code is more instructive than anything else. If you do want to use this code 
and get stuck then e-mail `roger.willis@r3.com`.

The repo is split into a number of modules:

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
3. You will need a working API key for the bank's API. If you don't have this 
   then you must start ed `daemon` in `mock-mode`.

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

Starling and Monzo sometimes change their API, if they do then this code will break.

Using the Mock Monzo bank account:

* Start the Daemon with the option `-mock-mode`.
* This way you can experiment with the functionality of the cash issuer
  without having to use a real bank account.
* The MockMonzo bank will create realistic-ish transactions at random
  intervals.

## Getting started

Start the corda nodes and issuer daemon:

1. Assuming all the API clients you need are implemented and a working
   config is present in the `resources` directory, then you are good to
   go! THe repo comes with a config file for Monzo and Starling. You just
   need to add your API key which has permission to view accounts, view transactions
   and check balances. If you don't have a Starling or Monzo account then use
   the daemon in `--mock-mode`
2. From the root of this repo run `./gradlew clean deployNodes`. The
   deployNodes script will build `Notary` `Issuer`, `PartyA` and `PartyB`
   nodes.
3. Navigate to the node directories `cd build/nodes`.
4. Run the nodes `./runnodes`.
5. Wait for all the nodes to start up.
6. Build the issuer daemon jar with `./gradlew :daemon:jar` the jar will be
   output to `daemon/build/libs/daemon-0.1.jar`
7. Start the issuer daemon (See "Starting the issuer daemon" below).
8. Start the issuer `service-ui` via IntelliJ. Run via the Green Arrow next to the
   `main` function in `com/r3/corda/finance/cash/issuer/Main.kt`. The app
   is defaulted to connect to the Issuer node on port 10006. This can be
   changed in `Main.kt` if required.

At this point all the required processes are up and running. Next, you can
perform a demo run of an issuance:

1. From `PartyA` add a new bank account via the node shell: `flow start Add bankAccount: { accountId: 12345, accountName: Rogers Account, accountNumber: { sortCode: “XXXXXX”, accountNumber: YYYYYYYY, type: uk }, currency: GBP }`
   replacing `XXXXXX` and `YYYYYYYY` with your sort code and account number.
   This is the bank account that you will make a payment from, to the issuer's
   account.
2. Next, we need to send the bank account you have just added, to the
   issuer node. First, we need to know the linear ID of the bank account
   state which has just been added: `run vaultQuery contractStateType: com.r3.corda.finance.cash.issuer.common.states.BankAccountState`.
   You should see the linear ID in the data structure which is output to the shell.
   Send the account to the issuer with `start Send issuer: Issuer, linearId: LINEAR_ID`.
3. You should see the issuer's UI update with new bank account information.
    Note: the issuer's account should already be added.
4. From the issuer daemon shell type `start`. The daemon should start
    polling for new transactions.
5. Make a payment (for a small amount!!) from `PartyA`s bank account to
    the `Issuer`s bank account. Soon after the payment has been made, the
    daemon should pick up the transaction information and the Issuer UI
    should update in the "nostro transactions" pane and the "node transactions"
    pane.
6. Assuming the correct details for the bank account used by PartyA were
    added and successfully sent to the issuer, then the issuance record in
    the node transaction tab should be marked as complete.
7. Run `run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State`
    from PartyA to inspect the amount of cash issued. It should be for
    the same amount of the payment sent to the issuer's account.

## Starting the issuer daemon

1. Start the daemon either via the main method in `Main.kt` from IntelliJ or 
   from the JAR created above with `java -jar daemon-0.1.jar`. The daemon should
   start and present you with a simple command line interface. The daemon
   requires a number of command line parameters. The main ones to know are:
   ```
   host-port    the host name and port of the code node to connect to
   rpcUser      the RPC username for the corda node
   rpcPass      the RPC password for the corda node
   ```
   All three of the above arguments are required. As such, note that if
   no corda node is available to connect to on the specified hostname and
   port, then the daemon will not start successfully.
   
   There are three other parameters to note:
   ```
   mock-mode - use this if you don't want to use a real bank account.
   auto-mode - Use this to start polling the bank accounts for new transactions as soon as the daemon startes.
   start-from - Use this flag to ignore all the past transactions in the bank account. This is useful if you want to perform a demo and need to re-use the same account multiple times but give the impression that the demo is from "scratch".
    ```
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

