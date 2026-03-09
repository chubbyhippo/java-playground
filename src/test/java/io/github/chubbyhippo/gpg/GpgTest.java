package io.github.chubbyhippo.gpg;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class GpgTest {

    @BeforeAll
    static void setUpProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void shouldEncryptAndDecryptMessage() throws Exception {
        char[] passphrase = "test-passphrase".toCharArray();
        String original = "hello gpg";

        PGPSecretKeyRing secretKeyRing = generateSecretKeyRing(passphrase);
        PGPPublicKey publicKey = findEncryptionKey(secretKeyRing);
        PGPSecretKey secretKey = findSecretKey(secretKeyRing);

        byte[] encrypted = encrypt(original.getBytes(StandardCharsets.UTF_8), publicKey);
        byte[] decrypted = decrypt(encrypted, secretKey, passphrase);

        assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo(original);
    }

    private static PGPSecretKeyRing generateSecretKeyRing(char[] passphrase)
            throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();
        JcaPGPKeyPair pgpKeyPair = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PGPPublicKey.RSA_GENERAL, keyPair, new Date());

        PGPDigestCalculator sha1 =
                new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);

        PGPKeyRingGenerator keyRingGenerator =
                new PGPKeyRingGenerator(
                        PGPSignature.POSITIVE_CERTIFICATION,
                        pgpKeyPair,
                        "test@example.com",
                        sha1,
                        null,
                        null,
                        new JcaPGPContentSignerBuilder(
                                pgpKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256)
                                .setProvider("BC"),
                        new JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha1)
                                .setProvider("BC")
                                .build(passphrase));

        return keyRingGenerator.generateSecretKeyRing();
    }

    private static PGPPublicKey findEncryptionKey(PGPSecretKeyRing keyRing) {
        Iterator<PGPPublicKey> keys = keyRing.getPublicKeys();
        while (keys.hasNext()) {
            PGPPublicKey key = keys.next();
            if (key.isEncryptionKey()) {
                return key;
            }
        }
        throw new IllegalStateException("No encryption key found");
    }

    private static PGPSecretKey findSecretKey(PGPSecretKeyRing keyRing) {
        Iterator<PGPSecretKey> keys = keyRing.getSecretKeys();
        while (keys.hasNext()) {
            PGPSecretKey key = keys.next();
            if (key.isSigningKey()) {
                return key;
            }
        }
        throw new IllegalStateException("No secret key found");
    }

    private static byte[] encrypt(byte[] clearData, PGPPublicKey encryptionKey) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ArmoredOutputStream armoredOutput = new ArmoredOutputStream(output)) {
            ByteArrayOutputStream compressedBuffer = new ByteArrayOutputStream();
            PGPCompressedDataGenerator compressor =
                    new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);

            try {
                try (var compressedOut = compressor.open(compressedBuffer)) {
                    PGPLiteralDataGenerator literalGenerator = new PGPLiteralDataGenerator();
                    try (var literalOut =
                                 literalGenerator.open(
                                         compressedOut,
                                         PGPLiteralData.BINARY,
                                         "data",
                                         clearData.length,
                                         new Date())) {
                        literalOut.write(clearData);
                    }
                }
            } finally {
                compressor.close();
            }

            byte[] compressedData = compressedBuffer.toByteArray();

            PGPEncryptedDataGenerator encryptedDataGenerator =
                    new PGPEncryptedDataGenerator(
                            new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                                    .setWithIntegrityPacket(true)
                                    .setSecureRandom(new SecureRandom())
                                    .setProvider("BC"));

            encryptedDataGenerator.addMethod(
                    new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey).setProvider("BC"));

            try (var encryptedOut = encryptedDataGenerator.open(armoredOutput, compressedData.length)) {
                encryptedOut.write(compressedData);
            }
        }

        return output.toByteArray();
    }

    private static byte[] decrypt(byte[] encryptedData, PGPSecretKey secretKey, char[] passphrase)
            throws Exception {
        PGPObjectFactory factory =
                new JcaPGPObjectFactory(PGPUtil.getDecoderStream(new ByteArrayInputStream(encryptedData)));

        Object object = factory.nextObject();
        PGPEncryptedDataList encryptedDataList =
                object instanceof PGPEncryptedDataList
                        ? (PGPEncryptedDataList) object
                        : (PGPEncryptedDataList) factory.nextObject();

        PGPPublicKeyEncryptedData encryptedPacket =
                (PGPPublicKeyEncryptedData) encryptedDataList.getEncryptedDataObjects().next();

        PBESecretKeyDecryptor decryptor =
                new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase);

        PGPPrivateKey privateKey = secretKey.extractPrivateKey(decryptor);

        try (var clearStream =
                     encryptedPacket.getDataStream(
                             new JcePublicKeyDataDecryptorFactoryBuilder()
                                     .setProvider("BC")
                                     .build(privateKey))) {

            JcaPGPObjectFactory plainFactory = new JcaPGPObjectFactory(clearStream);
            Object message = plainFactory.nextObject();

            if (message instanceof PGPCompressedData compressedData) {
                JcaPGPObjectFactory compressedFactory =
                        new JcaPGPObjectFactory(compressedData.getDataStream());
                message = compressedFactory.nextObject();
            }

            if (message instanceof PGPLiteralData literalData) {
                return literalData.getInputStream().readAllBytes();
            }

            if (message instanceof PGPOnePassSignatureList) {
                throw new PGPException("Signed messages are not handled in this test");
            }

            throw new PGPException("Unexpected PGP message type: " + message.getClass().getName());
        }
    }
}
