package com.hecochain.metatx.signer;

import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetaRawData {
    private byte[] nonce;
    private byte[] gasPrice;
    private byte[] gasLimit;
    private byte[] receiveAddress;
    private byte[] value;
    private byte[] data;
    protected byte[] sendAddress;
    private BigInteger feePercent;
    private BigInteger chainId;
    private BigInteger blockNumber;
    private byte[] rlpEncodeData;
    private byte[] rawHash;
    private boolean isEncode;

    public MetaRawData(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data, byte[] senderAddress, BigInteger feePercent, BigInteger blockNumber, BigInteger chainId) {
        this.chainId = chainId;
        this.nonce = Arrays.equals(nonce, ByteUtil.ZERO_BYTE_ARRAY) ? ByteUtil.EMPTY_BYTE_ARRAY : nonce;
        this.gasPrice = Arrays.equals(gasPrice, ByteUtil.ZERO_BYTE_ARRAY) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice;
        this.gasLimit = Arrays.equals(gasLimit, ByteUtil.ZERO_BYTE_ARRAY) ? ByteUtil.EMPTY_BYTE_ARRAY : gasLimit;
        this.receiveAddress = receiveAddress == null ? ByteUtil.EMPTY_BYTE_ARRAY : receiveAddress;
        this.value = Arrays.equals(value, ByteUtil.ZERO_BYTE_ARRAY) ? ByteUtil.EMPTY_BYTE_ARRAY : value;
        this.data = data == null ? ByteUtil.EMPTY_BYTE_ARRAY : data;
        this.chainId = chainId;
        this.feePercent = feePercent;
        this.sendAddress = senderAddress;
        this.blockNumber = blockNumber;
        this.isEncode = false;
        this.rlpEncodeData = null;
        this.rawHash = null;
    }

    private synchronized void rlpEncode() {
        if (!this.isEncode) {
            List<RlpType> result = new ArrayList();
            result.add(RlpString.create(nonce));
            result.add(RlpString.create(gasPrice));
            result.add(RlpString.create(gasLimit));
            result.add(RlpString.create(receiveAddress));

            result.add(RlpString.create(value));
            result.add(RlpString.create(data));
            result.add(RlpString.create(sendAddress));
            result.add(RlpString.create(feePercent));
            result.add(RlpString.create(blockNumber));
            result.add(RlpString.create(chainId));

            RlpList rlpList = new RlpList(result);
            this.rlpEncodeData = RlpEncoder.encode(rlpList);

            this.rawHash = HashUtil.sha3(this.rlpEncodeData);
            this.isEncode = true;
        }
    }

    public byte[] getEncodeData() {
        if (this.rlpEncodeData != null) {
            return this.rlpEncodeData;
        }
        this.rlpEncode();
        return this.rlpEncodeData;
    }

    public byte[] getHash() {
        if (this.rawHash != null) {
            return this.rawHash;
        }
        this.rlpEncode();
        return this.rawHash;
    }
}
