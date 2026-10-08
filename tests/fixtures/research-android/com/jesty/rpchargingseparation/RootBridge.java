package com.jesty.rpchargingseparation;
/** No Binder or device operations exist in the host printer fixture. */
final class RootBridge {
    static String exec(String command) { String receipt = System.getenv("JESTY_TEST_RECEIPT");
        if (receipt == null) throw new AssertionError("unexpected Binder call");
        return receipt; }
}
