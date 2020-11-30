import java.lang.reflect.Field;

public class FieldDemo {

    public static void main(String[] args) throws NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {

        Field field = SampleClass.class.getField("sampleField");
        System.out.println(field.getType().getName());
    }
}

class SampleClass {
    public static long sampleField = 5999;
}


