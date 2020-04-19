// package org.openhab.io.homekit.obsolete;
//
// import java.io.IOException;
// import java.math.BigInteger;
// import java.nio.charset.StandardCharsets;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
//
// import javax.servlet.ServletException;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;
// import javax.servlet.http.HttpSession;
//
// import org.apache.commons.io.IOUtils;
// import org.eclipse.jetty.server.Request;
// import org.openhab.io.homekit.api.AccessoryServer;
// import org.openhab.io.homekit.internal.servlet.HomekitServerSRP6Session;
// import org.openhab.io.homekit.internal.servlet.HomekitServerSRP6Session.State;
// import org.openhab.io.homekit.util.Message;
// import org.openhab.io.homekit.util.TypeLengthValue;
// import org.openhab.io.homekit.util.TypeLengthValue.Encoder;
//
// import com.nimbusds.srp6.ClientEvidenceRoutine;
// import com.nimbusds.srp6.SRP6ClientEvidenceContext;
// import com.nimbusds.srp6.SRP6CryptoParams;
// import com.nimbusds.srp6.SRP6ServerEvidenceContext;
// import com.nimbusds.srp6.SRP6VerifierGenerator;
// import com.nimbusds.srp6.ServerEvidenceRoutine;
// import com.nimbusds.srp6.XRoutineWithUserIdentity;
//
// public class PairSetupStageOneHandler extends PairSetupHandler {
//
// public PairSetupStageOneHandler(AccessoryServer server) {
// super(server);
// }
//
// @Override
// public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
// throws IOException, ServletException {
//
// byte[] body = IOUtils.toByteArray(request.getInputStream());
//
// if (getStage(body) == 1) {
// logger.info("Starting pair for " + server.getId());
//
// HttpSession session = request.getSession();
// HomekitServerSRP6Session SRP6Session = (HomekitServerSRP6Session) session.getAttribute("SRP6Session");
//
// if (SRP6Session == null) {
// SRP6Session = new HomekitServerSRP6Session(config);
// SRP6Session.setClientEvidenceRoutine(new ClientEvidenceRoutineImpl());
// SRP6Session.setServerEvidenceRoutine(new ServerEvidenceRoutineImpl());
// session.setAttribute("SRP6Session", SRP6Session);
// }
//
// if (SRP6Session.getState() != State.INIT) {
// logger.error("Session is not in state INIT when receiving step1");
// response.setStatus(HttpServletResponse.SC_CONFLICT);
// } else {
// SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(config);
// verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());
// BigInteger verifier = verifierGenerator.generateVerifier(server.getSalt(), IDENTIFIER,
// server.getSetupCode());
//
// Encoder encoder = TypeLengthValue.getEncoder();
// encoder.add(Message.STATE, (short) 0x02);
// encoder.add(Message.SALT, server.getSalt());
// encoder.add(Message.PUBLIC_KEY, SRP6Session.step1(IDENTIFIER, server.getSalt(), verifier));
//
// response.setContentType("application/pairing+tlv8");
// response.setStatus(HttpServletResponse.SC_OK);
// response.getOutputStream().write(encoder.toByteArray());
// response.getOutputStream().flush();
// }
//
// baseRequest.setHandled(true);
// }
// }
//
// class ServerEvidenceRoutineImpl implements ServerEvidenceRoutine {
//
// @Override
// public BigInteger computeServerEvidence(SRP6CryptoParams cryptoParams, SRP6ServerEvidenceContext ctx) {
//
// MessageDigest digest;
// try {
// digest = MessageDigest.getInstance(cryptoParams.H);
// } catch (NoSuchAlgorithmException e) {
// throw new RuntimeException("Could not locate requested algorithm", e);
// }
//
// byte[] hS = digest.digest(bigIntegerToUnsignedByteArray(ctx.S));
//
// digest.update(bigIntegerToUnsignedByteArray(ctx.A));
// digest.update(bigIntegerToUnsignedByteArray(ctx.M1));
// digest.update(hS);
//
// return new BigInteger(1, digest.digest());
// }
// }
//
// class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine {
//
// public ClientEvidenceRoutineImpl() {
// }
//
// /**
// * Calculates M1 according to the following formula:
// *
// * <p>
// * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
// */
// @Override
// public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx) {
//
// MessageDigest digest;
// try {
// digest = MessageDigest.getInstance(cryptoParams.H);
// } catch (NoSuchAlgorithmException e) {
// throw new RuntimeException("Could not locate requested algorithm", e);
// }
// digest.update(bigIntegerToUnsignedByteArray(cryptoParams.N));
// byte[] hN = digest.digest();
//
// digest.update(bigIntegerToUnsignedByteArray(cryptoParams.g));
// byte[] hg = digest.digest();
//
// byte[] hNhg = xor(hN, hg);
//
// digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
// byte[] hu = digest.digest();
//
// digest.update(bigIntegerToUnsignedByteArray(ctx.S));
// byte[] hS = digest.digest();
//
// digest.update(hNhg);
// digest.update(hu);
// digest.update(bigIntegerToUnsignedByteArray(ctx.s));
// digest.update(bigIntegerToUnsignedByteArray(ctx.A));
// digest.update(bigIntegerToUnsignedByteArray(ctx.B));
// digest.update(hS);
// BigInteger ret = new BigInteger(1, digest.digest());
// return ret;
// }
//
// private byte[] xor(byte[] b1, byte[] b2) {
// byte[] result = new byte[b1.length];
// for (int i = 0; i < b1.length; i++) {
// result[i] = (byte) (b1[i] ^ b2[i]);
// }
// return result;
// }
// }
//
// }
