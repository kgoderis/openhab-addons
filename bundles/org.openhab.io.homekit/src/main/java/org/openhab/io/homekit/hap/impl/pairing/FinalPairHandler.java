package org.openhab.io.homekit.hap.impl.pairing;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.openhab.io.homekit.hap.HomekitAuthInfo;
import org.openhab.io.homekit.hap.impl.crypto.ChachaDecoder;
import org.openhab.io.homekit.hap.impl.crypto.ChachaEncoder;
import org.openhab.io.homekit.hap.impl.crypto.EdsaSigner;
import org.openhab.io.homekit.hap.impl.crypto.EdsaVerifier;
import org.openhab.io.homekit.hap.impl.http.HttpResponse;
import org.openhab.io.homekit.hap.impl.pairing.PairSetupRequest.Stage3Request;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;

class FinalPairHandler {

    private final byte[] k;
    private final HomekitAuthInfo authInfo;

    private byte[] hkdf_enc_key;

    public FinalPairHandler(byte[] k, HomekitAuthInfo authInfo) {
        this.k = k;
        this.authInfo = authInfo;
    }

    public HttpResponse handle(PairSetupRequest req) throws Exception {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(k, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] okm = hkdf_enc_key = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        return decrypt((Stage3Request) req, okm);
    }

    private HttpResponse decrypt(Stage3Request req, byte[] key) throws Exception {
        ChachaDecoder chacha = new ChachaDecoder(key, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chacha.decodeCiphertext(req.getAuthTagData(), req.getMessageData());

        org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d = TypeLengthValue.decode(plaintext);
        byte[] username = d.getBytes(Message.IDENTIFIER);
        byte[] ltpk = d.getBytes(Message.PUBLIC_KEY);
        byte[] proof = d.getBytes(Message.SIGNATURE);
        return createUser(username, ltpk, proof);
    }

    private HttpResponse createUser(byte[] username, byte[] ltpk, byte[] proof) throws Exception {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(k, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] okm = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        byte[] completeData = ByteUtils.joinBytes(okm, username, ltpk);

        if (!new EdsaVerifier(ltpk).verify(completeData, proof)) {
            throw new Exception("Invalid signature");
        }
        authInfo.createUser(authInfo.getMac() + new String(username, StandardCharsets.UTF_8), ltpk);
        return createResponse();
    }

    private HttpResponse createResponse() throws Exception {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(k, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] okm = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        EdsaSigner signer = new EdsaSigner(authInfo.getPrivateKey());

        byte[] material = ByteUtils.joinBytes(okm, authInfo.getMac().getBytes(StandardCharsets.UTF_8),
                signer.getPublicKey());

        byte[] proof = signer.sign(material);

        org.openhab.io.homekit.util.TypeLengthValue.Encoder encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.IDENTIFIER, authInfo.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(Message.PUBLIC_KEY, signer.getPublicKey());
        encoder.add(Message.SIGNATURE, proof);
        byte[] plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(hkdf_enc_key, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValue.getEncoder();
        encoder.add(Message.STATE, (short) 6);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);

        return new PairingResponse(encoder.toByteArray());
    }
}
