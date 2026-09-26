package com.jesty.rpchargingseparation;

import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

final class RootBridge {
    private RootBridge() {}

    static String exec(String command) throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        Method getService = serviceManager.getMethod("getService", String.class);
        IBinder binder = (IBinder) getService.invoke(null, "PServerBinder");
        if (binder == null) throw new IllegalStateException("PServerBinder unavailable");

        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeStringArray(new String[]{command, "0"});
            if (!binder.transact(0, request, reply, 0)) {
                throw new IllegalStateException("PServerBinder rejected the command");
            }
            byte[] output = reply.createByteArray();
            return output == null ? "" : new String(output, StandardCharsets.UTF_8);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }
}
