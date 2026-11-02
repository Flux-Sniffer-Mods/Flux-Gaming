-keepattributes SourceFile,LineNumberTable
# Log calls below warnings dropped from release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
