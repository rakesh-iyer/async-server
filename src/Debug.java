import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public final class Debug {
    public static String print(Object object) {
        return ReflectionToStringBuilder.toString(object,
                ToStringStyle.MULTI_LINE_STYLE,
                true,
                true);
    }

    public static String printData(byte[] arr) {
        StringBuilder stringBuilder = new StringBuilder();

        for (byte b: arr) {
            stringBuilder.append(b + " ");
        }
        stringBuilder.append("");

        return stringBuilder.toString();
    }

}
