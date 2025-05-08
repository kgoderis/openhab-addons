// package org.openhab.io.homekit.api.hap;

// import java.util.EnumSet;
// import java.util.HashMap;
// import java.util.Map;

// public enum HomekitError {
//     RESERVED(0),
//     UNKNOWN(1),
//     AUTHENTICATION(2),
//     BACKOFF(3),
//     MAXPEERS(4),
//     MAXTRIES(5),
//     UNAVAILABLE(6),
//     BUSY(7);

//     private final short key;

//     private static final Map<Short, HomekitError> lookup = new HashMap<Short, HomekitError>();

//     static {
//         for (HomekitError s : EnumSet.allOf(HomekitError.class)) {
//             lookup.put(s.getKey(), s);
//         }
//     }

//     HomekitError(short key) {
//         this.key = key;
//     }

//     HomekitError(int key) {
//         this.key = (short) key;
//     }

//     public short getKey() {
//         return key;
//     }

//     public static HomekitError get(short code) {
//         return lookup.get(code);
//     }
// }
