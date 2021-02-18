package com.hecochain.metatx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hecochain.metatx.signer.MetaData;
import com.hecochain.metatx.signer.MetaTxService;
import com.hecochain.metatx.signer.Util;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.util.ByteUtil;
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

    public void encode(String tx) throws Exception {
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

    public void decode() throws Exception {
        Transaction tx = new Transaction(Hex.decode("f8d0820113843b9aca0082c3509488b7790f47e8875c0f8cf63e507c550586be55d688016345785d8a0000b860234d6574615472616e73616374696f6e23f84d831c96648207d0820124a0d98bcbc63448eebfe290d287cd63dcda129e0ae10c87aa10777909eeb336ac39a022510088aef0f7221f9a95575efe9476baf4e66560920891c8dca091aca0cb6e80820123a04d7f8d6a891b2b88297e97f6a09cb19e6713c49fc3f3bd7bf14a981833c03729a01250b7dfa70c9d74c9d010e6aeb03de1e8616b3074d297b2a08a15238e72114e"));
        String rawData = Hex.toHexString(tx.getData());
        System.out.println(rawData);
        MetaData metaData = new MetaData(ByteUtil.hexStringToBytes(rawData.substring("234d6574615472616e73616374696f6e23".length())));
        metaData.rlpPares();
        System.out.println("hh");
    }

}
