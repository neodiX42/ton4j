# BitString module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>org.ton.java</groupId>
    <artifactId>bitstring</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Jitpack [![JitPack][jitpack-svg]][jitpack]

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.neodiX42.ton-java-temp1</groupId>
    <artifactId>bitstring</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Usage example

```java
BitString bitString = new BitString(1023);
bitString.writeUint(BigInteger.valueOf(200), 8);
bitString.writeInt(BigInteger.valueOf(200), 9);
bitString.writeCoins(BigInteger.TEN);
bitString.writeString("A");
Address address = new Address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
bitString.writeAddress(address);
log.info(bitString.toString());
log.info(bitString.toBitString());

//result
C86408520C002CF55953E92EFBEADAB7BA725C3F93A0B23F842CBBA72D7B8E6F510A70E422E3
1100100001100100000010000101001000001100000000000010110011110101010110010101001111101001001011101111101111101010110110101011011110111010011100100101110000111111100100111010000010110010001111111000010000101100101110111010011100101101011110111000111001101111010100010000101001110000111001000010001011100011
```

More examples on how to work with `BitString` can be found in `TestBitString` class.


[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.java/bitstring
[maven-central]: https://mvnrepository.com/artifact/org.ton.java/bitstring
[jitpack-svg]: https://jitpack.io/v/neodiX42/ton-java.svg
[jitpack]: https://jitpack.io/#neodiX42/ton-java/bitstring
[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue
[ton]: https://ton.org