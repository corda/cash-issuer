Based on ![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Cash Issuer

** WARNING: This is a PoC level implementation that extends Cash Issuer implementation from Corda developers.

This code uses the `MockMonzo` client to generate
fake transaction data from the banks and this can be easily modified with real Bank API integrations on a later point of time.

This is an example of how to implement a cash issuer/cash tokenizer as
described in the [cash issuer design document](design/design.md).

The repo is split into a number of modules:

1. **client** - code which should be run by participants in a cash business
   network.
2. **common** - code which is shared by the cash issuer and users of cash states
   issued by the cash issuer. E.g. abstract flow initiator definitions
   and types.
3. **daemon** - a process which polls bank APIs for new transactions,
   transforms the data into a common format and sends it to the issuer
   node for processing. This at present also has the mock bank api implementation as well
4. **service** -Contains flows for processing data speficic to cash issuer node
   initiated by the daemon as well as flows for issuing and redeeming cash
   states
5. **service-ui** - a basic JavaFx app that provides a view on the cash issuer
   node.
6. **spring-boot-client** - spring boot web server api to connect with the other party nodes in the network
   

## Requirements

1. Three bank accounts. One for the Issuer, one for PartyA and one for
   partyB.
2. The bank holding the Issuer's bank account needs to offer a
   public API which allows clients to get account information, balance
   information and transaction information in real time.
3. You will need a working API key for the bank's API. If you don't have this 
   then you must start ed `daemon` in `mock-mode`.

## How to use issuer daemon


Currently we are using the Mock Monzo bank account:

* Start the Daemon with the option `-mock-mode` with the command: `java -jar daemon/build/libs/issuer-daemon-0.1.jar -mock-mode -host-port=localhost:10006
` assuming that the issuer node rpc port is `10006`
* This way you can experiment with the functionality of the cash issuer
  without having to use a real bank account.
* The MockMonzo bank will create realistic-ish transactions at random
  intervals.
* The transactions created by the MockMonzo bank come from five pre-defined bank 
  account numbers, which are:

        Account number: 13371337, sort code: 442200
        Account number: 12345678, sort code: 873456
        Account number: 73510753, sort code: 059015
        Account number: 34782115, sort code: 022346
        Account number: 90143578, sort code: 040040 
  
  We need to add one of these banks accounts to NodeA before starting the daemon. This is illustrated in the [Getting started](#-getting-started) section.


####Add your own bank API clients:

1. The daemon is extensible. Support for any bank HTTP API can be added by
   sub-classing `OpenBankingApiClient` and providing an interface definition
   for the API that can be used by Retrofit. Look at the [Monzo](./daemon/src/main/kotlin/com/allianz/t2i/issuer/daemon/clients/Monzo.kt)
   and [Starling](./daemon/src/main/kotlin/com/allianz/t2i/issuer/daemon/clients/Starling.kt)
   implementations as examples.
2. You will need to add a config file to the resources folder, For,
   example to add support for "Foo Bank", add a "FooBankClient" class that
   sub-classes `OpenBankingApiClient` and add a `foobank.conf` config file
   (omit 'Client' from the config file name). Config files contain three
   key/value pairs:

   ```
   apiBaseUrl="[URL HERE]"
   apiVersion=""
   apiAccessToken="[ADD ACCESS TOKEN HERE]"
   ```

## Getting started

Start the corda nodes and issuer daemon:

1. Assuming that we have not yet implemented the bank APIs for the daemon to poll for transactions, we use
   the daemon in `--mock-mode`
2. From the root of this repo run `./gradlew clean deployNodes`. The
   deployNodes script will build `Notary` `Issuer`, `PartyA` and `PartyB`
   nodes. Also it will build the jar for `daemon` and `service-ui`
3. start the nodes `./build/nodes/runnodes`.
5. Wait for all the nodes to start up.
6. Start the issuer daemon by running `java -jar daemon/build/libs/issuer-daemon-0.1.jar -mock-mode -host-port=localhost:10006`
7. Start the issuer `service-ui` within IntelliJ via the Green Arrow next to the
   `main` function in `com/allianz/t2i/issuer/ui/Main.kt`. 
8. Start the api server by running the command `./gradlew :spring-boot-client:runAPIServer`. This exposes REST APIs using Spring Api Server

At this point all the required processes are up and running. Next, you can
perform different actions by invoking the REST APIs

Detailed description of each API endpoints is given in the [Postman Collection](design/postman.json)

**Add Bank Account** 
1. From `PartyA` add a new bank account via the API call `http://localhost:10055/t2i/addbankaccount?node=PartyA&accountId=11111&accountName=AGCSSE&accountNumber=13371337&sortCode=442200`. 
The bank account number and sort code should be provided as any of the predefined ones if we are running in the mock mode. 

2. Next, we need to verify the bank account from the issuer node. The flow for enabling this functionality is not yet implemented. 

3. You should see the issuer's UI update with new bank account information.
    Note: the issuer's account should already be added.
    
 **Issue Token** 
4. From the issuer daemon shell type `start`. The daemon should start
    polling for new transactions. This will fetch new transactions and the daemon will start the flows that are for creating 
    `NostroTransactionState` and also from which when processed will create the `NodeTransactionState` corresponding to token issuance.
    
5. The Issuer UI should update in the "nostro transactions" pane and the "node transactions"
    pane showing the above states
    
6. Assuming the correct details for the bank account used by PartyA were
    added and successfully sent to the issuer, then the `NodeTransactionState` will trigger the flow for generating tokens of type `FiatCurrency`
    and the `NodeTransactionState` should be marked as complete.
    
7. Run ` run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken`
    from PartyA to inspect the amount of fungible tokens issued. It should be for
    the same amount of the payment sent to the issuer's account.
    
**Token Transfer**

1. From `PartyA` initiate the token transfer via the API call `http://localhost:10055/t2i/addbankaccount?node=PartyA&accountId=11111&accountName=AGCSSE&accountNumber=13371337&sortCode=442200`. 
   Provide the well known identity of the party to which token needs to be transferred as well as the quantity of token to be transferred.

2. After successful completion the result will be the updated token balance after the transfer

 
**Token Redemption**

1. For `PartyA` to initiate a redemption, use the API `http://localhost:10055/t2i/tokenredeem?node=PartyA&amount=10000`.
Provide the amount of token to be redeemed and the well known identity of `PartyA`

2. It will trigger the flow to destroy the required amount of UNCONSUMED tokens from Node A and then will create `NodeTransactionState` corresponding to the actual physical transfer required.

3. Once the bank transfer is completed, details of this transfer is polled by `Issuer Daemon` and this results in creation of  `NostroTransactionState` corresponding to the physical transfer.

4. The `NostroTransactionState` and `NodeTransactionState` corresponding to redemption is matched and results in converting the status of `NodeTransactionState` from `PENDING` to `COMPLETE`


**Token Balance**
1. Token balance at anytime can be fetched using the api `http://localhost:10055/t2i/tokenbalance?node=PartyA`
Provide the well known identity of the node as parameter. 

## Working with the issuer daemon

1. Start the daemon either via the main method in `Main.kt` from IntelliJ or 
   from the JAR created above with `java -jar daemon/build/libs/issuer-daemon-0.1.jar -mock-mode -host-port=localhost:10006`. The daemon should
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

