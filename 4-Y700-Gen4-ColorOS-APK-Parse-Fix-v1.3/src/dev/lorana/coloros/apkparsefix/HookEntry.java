package dev.lorana.coloros.apkparsefix;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "ColorOSApkParseFix";
    private static final String PARSER_CLASS =
            "com.android.internal.pm.pkg.parsing.ParsingPackageUtils";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"android".equals(lpparam.packageName) ||
                !"android".equals(lpparam.processName)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "android.content.res.AssetManager",
                    null,
                    "containsAllocatedTable",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!Boolean.TRUE.equals(param.getResult()) || !isPackageParseCall()) {
                                return;
                            }

                            try {
                                Object result = XposedHelpers.callMethod(
                                        param.thisObject, "getApkAssets");
                                if (!(result instanceof Object[])) {
                                    return;
                                }

                                boolean sawValidStagedArsc = false;
                                for (Object apkAsset : (Object[]) result) {
                                    if (apkAsset == null) {
                                        continue;
                                    }
                                    Object pathValue = XposedHelpers.callMethod(
                                            apkAsset, "getAssetPath");
                                    if (!(pathValue instanceof String)) {
                                        continue;
                                    }
                                    String path = (String) pathValue;
                                    if (!isStagedApk(path)) {
                                        continue;
                                    }

                                    int validation = validateResourcesArsc(path);
                                    if (validation < 0) {
                                        XposedBridge.log(TAG + ": refused bypass for invalid " + path);
                                        return;
                                    }
                                    if (validation > 0) {
                                        sawValidStagedArsc = true;
                                    }
                                }

                                if (sawValidStagedArsc) {
                                    param.setResult(Boolean.FALSE);
                                    XposedBridge.log(TAG +
                                            ": bypassed false allocated-table result for verified staged APK");
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": validation failed; preserving platform result");
                                XposedBridge.log(t);
                            }
                        }
                    });
            XposedBridge.log(TAG + ": hook installed in system_server");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to install hook");
            XposedBridge.log(t);
        }
    }

    private static boolean isPackageParseCall() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (PARSER_CLASS.equals(frame.getClassName()) &&
                    "parseBaseApk".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStagedApk(String path) {
        if (path == null || !path.startsWith("/data/app/vmdl")) {
            return false;
        }
        return path.endsWith("/base.apk") || path.endsWith("/base");
    }

    // 1 = resources.arsc exists and is valid, 0 = no table, -1 = invalid/unreadable.
    private static int validateResourcesArsc(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return -1;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length < 22) {
                return -1;
            }

            long searchStart = Math.max(0, length - 65557);
            int searchSize = (int) (length - searchStart);
            byte[] tail = new byte[searchSize];
            raf.seek(searchStart);
            raf.readFully(tail);

            int eocd = -1;
            for (int i = tail.length - 22; i >= 0; i--) {
                if (u32(tail, i) == 0x06054b50L) {
                    eocd = i;
                    break;
                }
            }
            if (eocd < 0) {
                return -1;
            }

            int entries = u16(tail, eocd + 10);
            long centralOffset = u32(tail, eocd + 16);
            raf.seek(centralOffset);

            byte[] fixed = new byte[46];
            for (int index = 0; index < entries; index++) {
                raf.readFully(fixed);
                if (u32(fixed, 0) != 0x02014b50L) {
                    return -1;
                }

                int method = u16(fixed, 10);
                int nameLength = u16(fixed, 28);
                int extraLength = u16(fixed, 30);
                int commentLength = u16(fixed, 32);
                long localOffset = u32(fixed, 42);
                byte[] nameBytes = new byte[nameLength];
                raf.readFully(nameBytes);
                String name = new String(nameBytes, StandardCharsets.UTF_8);

                if ("resources.arsc".equals(name)) {
                    if (method != 0) {
                        return -1;
                    }
                    long nextCentral = raf.getFilePointer() + extraLength + commentLength;
                    raf.seek(localOffset);
                    byte[] local = new byte[30];
                    raf.readFully(local);
                    if (u32(local, 0) != 0x04034b50L) {
                        return -1;
                    }
                    long dataOffset = localOffset + 30L +
                            u16(local, 26) + u16(local, 28);
                    raf.seek(nextCentral);
                    return (dataOffset & 3L) == 0 ? 1 : -1;
                }

                raf.seek(raf.getFilePointer() + extraLength + commentLength);
            }
            return 0;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    private static int u16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static long u32(byte[] data, int offset) {
        return ((long) data[offset] & 0xffL) |
                (((long) data[offset + 1] & 0xffL) << 8) |
                (((long) data[offset + 2] & 0xffL) << 16) |
                (((long) data[offset + 3] & 0xffL) << 24);
    }
}

