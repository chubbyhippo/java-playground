package io.github.chubbyhippo.gpg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
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
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
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

public class GpgTest {

  @BeforeAll
  static void setUpProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Test
  void shouldEncryptAndDecryptMessage() throws Exception {
    var passphrase = "test-passphrase".toCharArray();
    var original = "hello gpg";

    var secretKeyRing = generateSecretKeyRing(passphrase);
    var publicKey = findEncryptionKey(secretKeyRing);
    var secretKey = findSecretKey(secretKeyRing);

    var encrypted = encrypt(original.getBytes(StandardCharsets.UTF_8), publicKey);
    var decrypted = decrypt(encrypted, secretKey, passphrase);

    assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo(original);
  }

  private static PGPSecretKeyRing generateSecretKeyRing(char[] passphrase) throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA", "BC");
    generator.initialize(2048);

    var keyPair = generator.generateKeyPair();
    var pgpKeyPair =
        new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PGPPublicKey.RSA_GENERAL, keyPair, new Date());

    var sha1 = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);

    var keyRingGenerator =
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
    var keys = keyRing.getPublicKeys();
    while (keys.hasNext()) {
      var key = keys.next();
      if (key.isEncryptionKey()) {
        return key;
      }
    }
    throw new IllegalStateException("No encryption key found");
  }

  private static PGPSecretKey findSecretKey(PGPSecretKeyRing keyRing) {
    var keys = keyRing.getSecretKeys();
    while (keys.hasNext()) {
      var key = keys.next();
      if (key.isSigningKey()) {
        return key;
      }
    }
    throw new IllegalStateException("No secret key found");
  }

  private static byte[] encrypt(byte[] clearData, PGPPublicKey encryptionKey) throws Exception {
    var output = new ByteArrayOutputStream();

    try (var armoredOutput = new ArmoredOutputStream(output)) {
      var compressedBuffer = new ByteArrayOutputStream();
      var compressor = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);

      try {
        try (var compressedOut = compressor.open(compressedBuffer)) {
          var literalGenerator = new PGPLiteralDataGenerator();
          try (var literalOut =
              literalGenerator.open(
                  compressedOut, PGPLiteralData.BINARY, "data", clearData.length, new Date())) {
            literalOut.write(clearData);
          }
        }
      } finally {
        compressor.close();
      }

      var compressedData = compressedBuffer.toByteArray();

      var encryptedDataGenerator =
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

    var object = factory.nextObject();
    var encryptedDataList =
        object instanceof PGPEncryptedDataList
            ? (PGPEncryptedDataList) object
            : (PGPEncryptedDataList) factory.nextObject();

    var encryptedPacket =
        (PGPPublicKeyEncryptedData) encryptedDataList.getEncryptedDataObjects().next();

    var decryptor = new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase);

    var privateKey = secretKey.extractPrivateKey(decryptor);

    try (var clearStream =
        encryptedPacket.getDataStream(
            new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey))) {

      var plainFactory = new JcaPGPObjectFactory(clearStream);
      var message = plainFactory.nextObject();

      if (message instanceof PGPCompressedData compressedData) {
        var compressedFactory = new JcaPGPObjectFactory(compressedData.getDataStream());
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
