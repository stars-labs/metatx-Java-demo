package com.hecochain.metatx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hecochain.metatx.signer.MetaTxService;
import com.hecochain.metatx.signer.Util;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.ethereum.core.*;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

@Slf4j
public class MetaTxDemo {

    @Autowired
    private MetaTxService metaTxService;

    @Autowired
    private ObjectMapper objectMapper;

    public void getMetaSendResult(String tx) throws Exception {
        //your rpc
        Web3j web3j = Web3j.build(new HttpService("https://http-mainnet-node.huobichain.com"));

        int blockNumber = web3j.ethBlockNumber().send().getBlockNumber().intValue();

        Transaction transaction = new Transaction(Hex.decode(Util.removeHexPrefix(tx)));
        String from = Hex.toHexString(transaction.getSender());
        BigInteger balance = web3j.ethGetBalance(Util.addHexPrefix(from), DefaultBlockParameterName.LATEST).send().getBalance();

        Integer feePercent = 1000;//1000/10000
        String privateKey = "acdd38021ad3d7400aba48dec54a2a21d0f5196bb4835a0458ffe930f99afed3";//your private key, the account should have balance
        int chainId = 128;//mainnet 128; testnet 256
        int blockExp = blockNumber + 100; // set a expire block

        Transaction metaTransaction = metaTxService.getMetaSignedRawTx(tx, feePercent, privateKey, chainId, blockExp);

        String metaRawTx = "0x" + Hex.toHexString(metaTransaction.getEncoded());
        log.debug("raw tx : {}, meta tx : {}", tx, metaRawTx);

        //send this to the network or response to user

        //  EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(metaRawTx).send();
    }

}
