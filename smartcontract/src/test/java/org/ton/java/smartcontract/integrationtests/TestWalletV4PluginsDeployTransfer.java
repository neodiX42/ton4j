package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.java.smartcontract.wallet.v4.WalletV4ContractR2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV4PluginsDeployTransfer {

    @Test
    public void testPlugins() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = new Options();
        options.publicKey = keyPair.getPublicKey();
        options.wc = 0L;
        options.subscriptionConfig = SubscriptionInfo.builder()
                .beneficiary(Address.of("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka"))
                .subscriptionFee(Utils.toNano(2))
                .period(60)
                .startTime(0)
                .timeOut(30)
                .lastPaymentTime(0)
                .lastRequestTime(0)
                .failedAttempts(0)
                .subscriptionId(12345)
                .build();

        Wallet wallet = new Wallet(WalletVersion.v4R2, options);
        WalletV4ContractR2 contract = wallet.create();

        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());
        Address walletAddress = msg.address;

        String nonBounceableAddress = walletAddress.toString(true, true, false, true);
        String bounceableAddress = walletAddress.toString(true, true, true, true);

        String my = "\nCreating new advanced wallet V4 with plugins in workchain " + options.wc + "\n" +
                "with unique wallet id " + options.walletId + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "StateInit: " + msg.stateInit.print() + "\n" +
                "new wallet address = " + walletAddress.toString(false) + "\n" +
                "(Saving address to file new-wallet.addr)" + "\n" +
                "Non-bounceable address (for init): " + nonBounceableAddress + "\n" +
                "Bounceable address (for later access): " + bounceableAddress + "\n" +
                "signing message: " + msg.signingMessage.print() + "\n" +
                "External message for initialization is " + msg.message.print() + "\n" +
                Utils.bytesToHex(msg.message.toBoc(false)).toUpperCase() + "\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        // top up new wallet using test-faucet-wallet
        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .build();
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(10));
        log.info("new wallet balance: {}", Utils.formatNanoValue(balance));

        // deploy wallet-v4
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));

        //check if state of the new contract/wallet has changed from un-init to active
        FullAccountState state;
        do {
            TimeUnit.SECONDS.sleep(5);
            state = tonlib.getAccountState(walletAddress);
        } while (state.getAccount_state().getCode() == null);

        TimeUnit.SECONDS.sleep(5);

        long walletCurrentSeqno = contract.getSeqno(tonlib);
        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));
        log.info("seqno: {}", walletCurrentSeqno);
        log.info("subWalletId: {}", contract.getWalletId(tonlib));
        log.info("pubKey: {}", contract.getPublicKey(tonlib));
        log.info("pluginsList: {}", contract.getPluginsList(tonlib));

        // create and deploy plugin ------- start -----------------

        do {
            TimeUnit.SECONDS.sleep(5);
            state = tonlib.getAccountState(options.subscriptionConfig.getBeneficiary());
        } while (state.getAccount_state().getCode() == null);

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        NewPlugin plugin = NewPlugin.builder()
                .secretKey(keyPair.getSecretKey())
                .seqno(walletCurrentSeqno)
                .pluginWc(options.wc)
                .amount(Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                .stateInit(contract.createPluginStateInit())
                .body(contract.createPluginBody())
                .build();

        contract.deployAndInstallPlugin(tonlib, plugin);

        TimeUnit.SECONDS.sleep(20);

        // create and deploy plugin -------- end ----------------

        // get plugin list
        List<String> plugins = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", plugins.get(0));

        Address pluginAddress = Address.of(plugins.get(0));
        log.info("pluginAddress {}", pluginAddress.toString(false));

        SubscriptionInfo subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);

        log.info("{}", subscriptionInfo);

        log.info("plugin {} installed {}", pluginAddress, contract.isPluginInstalled(tonlib, pluginAddress));

        // Collect very first service fee
        Cell header = Contract.createExternalMessageHeader(pluginAddress);
        Cell extMessage = Contract.createCommonMsgInfo(header, null, null); // dummy external message, only destination address is relevant
        String extMessageBase64boc = Utils.bytesToBase64(extMessage.toBoc(false));
        tonlib.sendRawMessage(extMessageBase64boc);

        TimeUnit.SECONDS.sleep(20);

        do {
            TimeUnit.SECONDS.sleep(5);
            state = tonlib.getAccountState(options.subscriptionConfig.getBeneficiary());
        } while (state.getAccount_state().getCode() == null);

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        do {
            TimeUnit.SECONDS.sleep(5);
            state = tonlib.getAccountState(walletAddress);
        } while (state.getAccount_state().getCode() == null);

        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));

        subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);
        log.info("{}", subscriptionInfo);

        assertThat(subscriptionInfo.getLastPaymentTime()).isNotEqualTo(0);

        //------------ collect fee again
        TimeUnit.SECONDS.sleep(90);

        header = Contract.createExternalMessageHeader(pluginAddress);
        extMessage = Contract.createCommonMsgInfo(header, null, null);
        extMessageBase64boc = Utils.bytesToBase64(extMessage.toBoc(false));
        tonlib.sendRawMessage(extMessageBase64boc);

        do {
            TimeUnit.SECONDS.sleep(5);
            state = tonlib.getAccountState(options.subscriptionConfig.getBeneficiary());
        } while (state.getAccount_state().getCode() == null);

        log.info("beneficiaryWallet balance {}", Utils.formatNanoValue(state.getBalance()));

        do {
            TimeUnit.SECONDS.sleep(5);
            state = tonlib.getAccountState(walletAddress);
        } while (state.getAccount_state().getCode() == null);

        log.info("walletV4 balance: {}", Utils.formatNanoValue(state.getBalance()));

        subscriptionInfo = contract.getSubscriptionData(tonlib, pluginAddress);
        log.info("{}", subscriptionInfo);

        log.info("Uninstalling plugin {}", Address.of(contract.getPluginsList(tonlib).get(0)));

        // uninstall plugin -- start
        walletCurrentSeqno = contract.getSeqno(tonlib);
        DeployedPlugin deployedPlugin = DeployedPlugin.builder()
                .seqno(walletCurrentSeqno)
                .amount(Utils.toNano(0.1))
                .pluginAddress(Address.of(contract.getPluginsList(tonlib).get(0)))
                .secretKey(keyPair.getSecretKey())
                .queryId(0)
                .build();

        ExternalMessage extMsgRemovePlugin = contract.removePlugin(deployedPlugin);
        String extMsgRemovePluginBase64boc = Utils.bytesToBase64(extMsgRemovePlugin.message.toBoc(false));
        tonlib.sendRawMessage(extMsgRemovePluginBase64boc);
        // uninstall plugin -- end

        TimeUnit.SECONDS.sleep(20);
        List<String> list = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", list);
        assertThat(list.isEmpty()).isTrue();
    }
}