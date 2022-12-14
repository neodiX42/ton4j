package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ChannelData {
    long state;
    BigInteger balanceA;
    BigInteger balanceB;
    byte[] publicKeyA;
    byte[] publicKeyB;
    BigInteger channelId;
    long quarantineDuration;
    BigInteger misbehaviorFine;
    long conditionalCloseDuration;
    BigInteger seqnoA;
    BigInteger seqnoB;
    Cell quarantine;
    BigInteger excessFee;
    Address addressA;
    Address addressB;
}
