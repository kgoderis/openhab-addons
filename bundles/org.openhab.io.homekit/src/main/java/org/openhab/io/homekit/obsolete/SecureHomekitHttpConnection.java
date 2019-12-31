package org.openhab.io.homekit.obsolete;
// package org.openhab.io.homekit.internal.http;
//
// import java.nio.ByteBuffer;
// import java.util.concurrent.Executor;
//
// import org.eclipse.jetty.io.AbstractConnection;
// import org.eclipse.jetty.io.ByteBufferPool;
// import org.eclipse.jetty.io.Connection;
// import org.eclipse.jetty.io.EndPoint;
// import org.eclipse.jetty.util.Callback;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
/// **
// * A Connection that acts as an interceptor between an EndPoint providing SSL encrypted data
// * and another consumer of an EndPoint (typically an {@link Connection} like HttpConnection) that
// * wants unencrypted data.
// * <p>
// * The connector uses an {@link EndPoint} (typically SocketChannelEndPoint) as
// * it's source/sink of encrypted data. It then provides an endpoint via {@link #getDecryptedEndPoint()} to
// * expose a source/sink of unencrypted data to another connection (eg HttpConnection).
// * <p>
// * The design of this class is based on a clear separation between the passive methods, which do not block nor
// schedule
// * any
// * asynchronous callbacks, and active methods that do schedule asynchronous callbacks.
// * <p>
// * The passive methods are {@link DecryptedEndPoint#fill(ByteBuffer)} and
// * {@link DecryptedEndPoint#flush(ByteBuffer...)}. They make best
// * effort attempts to progress the connection using only calls to the encrypted {@link EndPoint#fill(ByteBuffer)} and
// * {@link EndPoint#flush(ByteBuffer...)}
// * methods. They will never block nor schedule any readInterest or write callbacks. If a fill/flush cannot progress
// * either because
// * of network congestion or waiting for an SSL handshake message, then the fill/flush will simply return with zero
// bytes
// * filled/flushed.
// * Specifically, if a flush cannot proceed because it needs to receive a handshake message, then the flush will
// attempt
// * to fill bytes from the
// * encrypted endpoint, but if insufficient bytes are read it will NOT call {@link EndPoint#fillInterested(Callback)}.
// * <p>
// * It is only the active methods : {@link DecryptedEndPoint#fillInterested(Callback)} and
// * {@link DecryptedEndPoint#write(Callback, ByteBuffer...)} that may schedule callbacks by calling the encrypted
// * {@link EndPoint#fillInterested(Callback)} and {@link EndPoint#write(Callback, ByteBuffer...)}
// * methods. For normal data handling, the decrypted fillInterest method will result in an encrypted fillInterest and a
// * decrypted
// * write will result in an encrypted write. However, due to SSL handshaking requirements, it is also possible for a
// * decrypted fill
// * to call the encrypted write and for the decrypted flush to call the encrypted fillInterested methods.
// * <p>
// * MOST IMPORTANTLY, the encrypted callbacks from the active methods (#onFillable() and WriteFlusher#completeWrite())
// do
// * no filling or flushing
// * themselves. Instead they simple make the callbacks to the decrypted callbacks, so that the passive encrypted
// * fill/flush will
// * be called again and make another best effort attempt to progress the connection.
// */
//
// public class SecureHomekitHttpConnection extends AbstractConnection implements Connection.UpgradeTo {
//
// protected static final Logger logger = LoggerFactory.getLogger(SecureHomekitHttpConnection.class);
//
// private final DecryptedHomekitEndPoint decryptedHomekitEndPoint;
//
// public SecureHomekitHttpConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint,
// byte[] readKey, byte[] writeKey, boolean useDirectBuffers) {
// super(endPoint, executor);
// this.decryptedHomekitEndPoint = new DecryptedHomekitEndPoint(endPoint, executor, byteBufferPool,
// useDirectBuffers, readKey, writeKey);
// }
//
// public DecryptedHomekitEndPoint getDecryptedEndPoint() {
// return decryptedHomekitEndPoint;
// }
//
// @Override
// public void onUpgradeTo(ByteBuffer buffer) {
// decryptedHomekitEndPoint.onUpgradeTo(buffer);
// }
//
// @Override
// public void onOpen() {
// super.onOpen();
// decryptedHomekitEndPoint.getConnection().onOpen();
// }
//
// @Override
// public void onClose() {
// decryptedHomekitEndPoint.getConnection().onClose();
// super.onClose();
// }
//
// @Override
// public void close() {
// decryptedHomekitEndPoint.getConnection().close();
// }
//
// @Override
// public boolean onIdleExpired() {
// return decryptedHomekitEndPoint.getConnection().onIdleExpired();
// }
//
// @Override
// public void onFillable() {
// // // onFillable means that there are encrypted bytes ready to be filled.
// // // however we do not fill them here on this callback, but instead wakeup
// // // the decrypted readInterest and/or writeFlusher so that they will attempt
// // // to do the fill and/or flush again and these calls will do the actually
// // // filling.
// //
// // if (logger.isDebugEnabled()) {
// // logger.debug("OnFillable : Start [{}]", SecureHomekitHttpConnection.this);
// // }
// //
// // // We have received a close handshake, close the end point to send FIN.
// // if (decryptedHomekitEndPoint.isInputShutdown()) {
// // decryptedHomekitEndPoint.close();
// // }
// //
// // decryptedHomekitEndPoint.onFillable();
// //
// // if (logger.isDebugEnabled()) {
// // logger.debug("OnFillable : End [{}]", SecureHomekitHttpConnection.this);
// // }
//
// decryptedHomekitEndPoint.getFillInterest().fillable();
// }
//
// @Override
// public void onFillInterestedFailed(Throwable cause) {
// // if (logger.isDebugEnabled()) {
// // logger.debug("onFillInterestedFailed : Start [{}]", SecureHomekitHttpConnection.this);
// // }
// //
// // decryptedHomekitEndPoint.onFillInterestedFailed(cause == null ? new IOException() : cause);
// //
// // if (logger.isDebugEnabled()) {
// // logger.debug("onFillInterestedFailed : End [{}]", SecureHomekitHttpConnection.this);
// // }
// decryptedHomekitEndPoint.getFillInterest().onFail(cause);
// }
// }
