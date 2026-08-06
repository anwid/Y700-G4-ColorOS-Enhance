package android.os;

public abstract class VibrationEffect implements Parcelable {
    public static VibrationEffect createOneShot(long milliseconds, int amplitude) { return null; }
    public static VibrationEffect createPredefined(int effectId) { return null; }
    public static VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat) { return null; }
}
