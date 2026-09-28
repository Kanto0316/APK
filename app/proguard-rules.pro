# Room entities and DAOs are discovered through generated code; no broad application keep
# rules are required. Android components are retained by the Android Gradle plugin.

# Release builds must not retain diagnostic Logcat calls or their message construction.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
