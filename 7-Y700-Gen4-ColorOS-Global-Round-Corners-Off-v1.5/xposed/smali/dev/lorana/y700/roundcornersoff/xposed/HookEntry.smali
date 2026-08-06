.class public Ldev/lorana/y700/roundcornersoff/xposed/HookEntry;
.super Ljava/lang/Object;

.implements Lde/robv/android/xposed/IXposedHookLoadPackage;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public handleLoadPackage(Lde/robv/android/xposed/callbacks/XC_LoadPackage$LoadPackageParam;)V
    .locals 7

    iget-object v0, p1, Lde/robv/android/xposed/callbacks/XC_LoadPackage$LoadPackageParam;->packageName:Ljava/lang/String;
    const-string v1, "com.android.systemui"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :return

    :try_start
    const-string v0, "android.app.Instrumentation"
    const/4 v1, 0x0
    const-string v2, "newApplication"
    const/4 v3, 0x4
    new-array v3, v3, [Ljava/lang/Object;
    const-class v4, Ljava/lang/ClassLoader;
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-class v4, Ljava/lang/String;
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const-class v4, Landroid/content/Context;
    const/4 v5, 0x2
    aput-object v4, v3, v5
    new-instance v4, Ldev/lorana/y700/roundcornersoff/xposed/HookEntry$1;
    invoke-direct {v4}, Ldev/lorana/y700/roundcornersoff/xposed/HookEntry$1;-><init>()V
    const/4 v5, 0x3
    aput-object v4, v3, v5
    invoke-static {v0, v1, v2, v3}, Lde/robv/android/xposed/XposedHelpers;->findAndHookMethod(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;[Ljava/lang/Object;)Lde/robv/android/xposed/XC_MethodHook$Unhook;
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch
    goto :return

    :catch
    move-exception v0
    invoke-static {v0}, Lde/robv/android/xposed/XposedBridge;->log(Ljava/lang/Throwable;)V

    :return
    return-void
.end method

.method public static hookFactories(Ljava/lang/ClassLoader;)V
    .locals 1
    const-string v0, "com.android.systemui.decor.RoundedCornerDecorProviderFactory"
    invoke-static {v0, p0}, Ldev/lorana/y700/roundcornersoff/xposed/HookEntry;->hookFactory(Ljava/lang/String;Ljava/lang/ClassLoader;)V
    const-string v0, "com.oplus.systemui.statusbar.corner.OplusRoundedCornerDecorProviderFactory"
    invoke-static {v0, p0}, Ldev/lorana/y700/roundcornersoff/xposed/HookEntry;->hookFactory(Ljava/lang/String;Ljava/lang/ClassLoader;)V
    return-void
.end method

.method private static hookFactory(Ljava/lang/String;Ljava/lang/ClassLoader;)V
    .locals 6
    :try_start
    const-string v0, "getHasProviders"
    const/4 v1, 0x0
    invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
    move-result-object v2
    invoke-static {v2}, Lde/robv/android/xposed/XC_MethodReplacement;->returnConstant(Ljava/lang/Object;)Lde/robv/android/xposed/XC_MethodReplacement;
    move-result-object v2
    const/4 v3, 0x1
    new-array v4, v3, [Ljava/lang/Object;
    aput-object v2, v4, v1
    invoke-static {p0, p1, v0, v4}, Lde/robv/android/xposed/XposedHelpers;->findAndHookMethod(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;[Ljava/lang/Object;)Lde/robv/android/xposed/XC_MethodHook$Unhook;

    const-string v0, "getProviders"
    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
    move-result-object v2
    invoke-static {v2}, Lde/robv/android/xposed/XC_MethodReplacement;->returnConstant(Ljava/lang/Object;)Lde/robv/android/xposed/XC_MethodReplacement;
    move-result-object v2
    new-array v4, v3, [Ljava/lang/Object;
    aput-object v2, v4, v1
    invoke-static {p0, p1, v0, v4}, Lde/robv/android/xposed/XposedHelpers;->findAndHookMethod(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;[Ljava/lang/Object;)Lde/robv/android/xposed/XC_MethodHook$Unhook;

    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "Y700CornersOff: hooked "
    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Lde/robv/android/xposed/XposedBridge;->log(Ljava/lang/String;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch
    goto :return

    :catch
    move-exception v0
    invoke-static {v0}, Lde/robv/android/xposed/XposedBridge;->log(Ljava/lang/Throwable;)V
    :return
    return-void
.end method
