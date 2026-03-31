package com.apexmatch.engine.golang;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface GolangNativeLibrary extends Library {

    static GolangNativeLibrary load(String libraryPath) {
        return Native.load(libraryPath, GolangNativeLibrary.class);
    }

    void engine_init(String symbol);

    Pointer engine_submit_order_json(String orderJson);

    byte engine_cancel_order(String symbol, long orderId);

    Pointer engine_get_depth_json(String symbol, int levels);

    void engine_free_string(Pointer ptr);
}
