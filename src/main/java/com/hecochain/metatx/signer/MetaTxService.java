package com.hecochain.metatx.signer;

import org.bouncycastle.util.encoders.Hex;
import org.ethereum.core.Transaction;
import org.ethereum.util.ByteUtil;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class MetaTxService {

    public Transaction getMetaSignedRawTx(String rawTx, int feePercent, String privateKey, int chainId, int blockNumber) {
        Transaction transaction = new Transaction(Hex.decode(Util.removeHexPrefix(rawTx)));
        Sign.SignatureData signatureData = this.getMetaSignatureData(transaction, feePercent, privateKey, chainId, blockNumber);
        String metaData = this.getMetaData(transaction, blockNumber, feePercent, signatureData);

        Transaction transaction1 = new Transaction(
                transaction.getNonce(),
                transaction.getGasPrice(),
                transaction.getGasLimit(),
                transaction.getReceiveAddress(),
                transaction.getValue(),
                ByteUtil.hexStringToBytes(metaData),
                ByteUtil.bigIntegerToBytes(transaction.getSignature().r),
                ByteUtil.bigIntegerToBytes(transaction.getSignature().s),
                transaction.getSignature().v,
                transaction.getChainId());

//        return "0x" + Hex.toHexString(transaction1.getEncoded());
        return transaction1;

    }

    private Sign.SignatureData getMetaSignatureData(Transaction transaction, int feePercent, String privateKey, int chainId, int blockNumber) {
        //(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data, byte[] senderAddress, BigInteger feePercent, BigInteger blockNumber, BigInteger chainId) {
        MetaRawData metaRawData = new MetaRawData(
                transaction.getNonce(),
                transaction.getGasPrice(),
                transaction.getGasLimit(),
                transaction.getReceiveAddress(),
                transaction.getValue(),
                transaction.getData(),
                transaction.getSender(),
                BigInteger.valueOf(feePercent),
                BigInteger.valueOf(blockNumber),
                BigInteger.valueOf(chainId)
        );
        byte[] encodeData = metaRawData.getEncodeData();
        Sign.SignatureData eip155SignatureData = this.getSignatureData(encodeData, privateKey, chainId);

        return eip155SignatureData;

    }

    private Sign.SignatureData getSignatureData(byte[] encodeData, String privateKey, int chainId) {
        ECKeyPair aPair = ECKeyPair.create(Numeric.toBigInt(privateKey));
        Sign.SignatureData signatureData = Sign.signMessage(encodeData, aPair);
        Sign.SignatureData eip155SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, chainId);

        return eip155SignatureData;
    }

    private String getMetaData(Transaction transaction, int blockNumber, int feePercent, Sign.SignatureData signatureData) {
        MetaData metaData = new MetaData(
                transaction.getData(),
                BigInteger.valueOf(blockNumber),
                BigInteger.valueOf(feePercent),
                signatureData.getV(),
                signatureData.getR(),
                signatureData.getS()
        );

        byte[] encodeData = metaData.getRlpEncoded();

        return Constant.META_DATA_PREFIX + Hex.toHexString(encodeData);
    }

}
