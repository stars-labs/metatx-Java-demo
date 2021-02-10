package com.hecochain.metatx.signer;

import org.ethereum.util.ByteUtil;
import org.web3j.crypto.Sign;
import org.web3j.rlp.*;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MetaData {

    private BigInteger blockNumLimit;

    private BigInteger feePercent;

    private byte[] R;

    private byte[] S;

    private byte[] V;

    private byte[] data;

    private byte[] rlpEncoded;

    private Sign.SignatureData signatureData;

    protected boolean parsed;

    public MetaData(byte[] rawData) {
        this.rlpEncoded = rawData;
        this.parsed = false;
    }

    public MetaData(byte[] data, BigInteger blockNumLimit, BigInteger feePercent, byte[] V, byte[] R, byte[] S) {
        this.data =  data == null ? ByteUtil.EMPTY_BYTE_ARRAY : data;
        this.blockNumLimit = blockNumLimit;
        this.feePercent = feePercent;
        this.V = V;
        this.S = S;
        this.R = R;
        this.parsed = true;
        this.rlpEncoded = null;
    }

    public byte[] getRlpEncoded() {
        if (this.rlpEncoded != null) {
            return this.rlpEncoded;
        } else {
            List<RlpType> finalResult = new ArrayList();

            finalResult.add(RlpString.create(blockNumLimit));
            finalResult.add(RlpString.create(feePercent));
            finalResult.add(RlpString.create(Bytes.trimLeadingZeroes(this.V)));
            finalResult.add(RlpString.create(Bytes.trimLeadingZeroes(this.R)));
            finalResult.add(RlpString.create(Bytes.trimLeadingZeroes(this.S)));
            finalResult.add(RlpString.create(data));

            RlpList finalRlpList = new RlpList(finalResult);
            byte[] finalEncodeData = RlpEncoder.encode(finalRlpList);
            this.rlpEncoded = finalEncodeData;
            return finalEncodeData;
        }
    }

    public synchronized void rlpPares() throws Exception {
        if (!this.parsed) {
            RlpList list = RlpDecoder.decode(this.rlpEncoded);
            RlpList values = (RlpList) list.getValues().get(0);

            if (values.getValues().size() < 6) {
                throw new Exception("rlp length does not right");
            }

            this.blockNumLimit = ((RlpString) values.getValues().get(0)).asPositiveBigInteger();
            this.feePercent = ((RlpString) values.getValues().get(1)).asPositiveBigInteger();

            this.V = Numeric.toBytesPadded(
                    Numeric.toBigInt(((RlpString) values.getValues().get(2)).getBytes()), 32);
            this.R = Numeric.toBytesPadded(
                    Numeric.toBigInt(((RlpString) values.getValues().get(3)).getBytes()), 32);
            this.S = Numeric.toBytesPadded(
                    Numeric.toBigInt(((RlpString) values.getValues().get(4)).getBytes()), 32);

            this.data = ((RlpString) values.getValues().get(5)).getBytes();

            this.signatureData = new Sign.SignatureData(V, R, S);
            this.parsed = true;
        }
    }

    public BigInteger getFeePercent() {
        try {
            this.rlpPares();
        } catch (Exception e) {
            return null;
        }
        return this.feePercent;
    }

    public byte[] getData() {
        try {
            this.rlpPares();
        } catch (Exception e) {
            return null;
        }
        return this.data;
    }

    public BigInteger getBlockNumLimit() {
        try {
            this.rlpPares();
        } catch (Exception e) {
            return null;
        }

        return this.blockNumLimit;
    }

    public Sign.SignatureData getSignatureData() {
        try {
            this.rlpPares();
        } catch (Exception e) {
            return null;
        }

        return this.signatureData;
    }

    public BigInteger getRealV(BigInteger chainId) {
        try {
            this.rlpPares();
        } catch (Exception e) {
            return null;
        }

        BigInteger chainIDMul = new BigInteger("2").multiply(chainId);
        return new BigInteger(1, this.signatureData.getV()).subtract(BigInteger.valueOf(35)).subtract(chainIDMul);
    }

}
