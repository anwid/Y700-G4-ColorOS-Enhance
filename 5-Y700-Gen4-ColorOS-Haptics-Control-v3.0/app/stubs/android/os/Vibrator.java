package android.os;

public abstract class Vibrator {
    public abstract void vibrate(VibrationEffect effect);
    public abstract void cancel();
}
