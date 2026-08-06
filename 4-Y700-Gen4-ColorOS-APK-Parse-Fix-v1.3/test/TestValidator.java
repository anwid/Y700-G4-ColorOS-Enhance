import java.lang.reflect.Method;

public final class TestValidator {
    public static void main(String[] args) throws Exception {
        Class<?> hook = Class.forName("dev.lorana.coloros.apkparsefix.HookEntry");
        Method validate = hook.getDeclaredMethod("validateResourcesArsc", String.class);
        validate.setAccessible(true);
        for (String path : args) {
            System.out.println(path + " => " + validate.invoke(null, path));
        }
    }
}
