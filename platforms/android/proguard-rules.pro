# Apache MINA SSHD — keep all classes (minify disabled in debug, but future-proof)
-keep class org.apache.sshd.** { *; }
-keep class net.i2p.crypto.eddsa.** { *; }
-keep class org.bouncycastle.** { *; }

# Keep JavascriptInterface methods
-keepclassmembers class com.bastion.app.terminal.TerminalBridge {
    @android.webkit.JavascriptInterface <methods>;
}
