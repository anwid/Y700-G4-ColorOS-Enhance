.class final Ldev/lorana/y700/roundcornersoff/xposed/HookEntry$1;
.super Lde/robv/android/xposed/XC_MethodHook;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Lde/robv/android/xposed/XC_MethodHook;-><init>()V
    return-void
.end method

.method protected afterHookedMethod(Lde/robv/android/xposed/XC_MethodHook$MethodHookParam;)V
    .locals 2
    :try_start
    invoke-virtual {p1}, Lde/robv/android/xposed/XC_MethodHook$MethodHookParam;->getResult()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/app/Application;
    invoke-virtual {v0}, Landroid/app/Application;->getClassLoader()Ljava/lang/ClassLoader;
    move-result-object v0
    invoke-static {v0}, Ldev/lorana/y700/roundcornersoff/xposed/HookEntry;->hookFactories(Ljava/lang/ClassLoader;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch
    goto :return
    :catch
    move-exception v0
    invoke-static {v0}, Lde/robv/android/xposed/XposedBridge;->log(Ljava/lang/Throwable;)V
    :return
    return-void
.end method
