package vn.edu.fpt.sba.music4.util;

import java.util.Arrays;

public class EnumUtil {

    public static <E extends Enum<E>> boolean isNotInEnum(Class<E> enumClass, String value) {
        if (value == null) return true;

        return Arrays.stream(enumClass.getEnumConstants())
                .noneMatch(e -> e.name().equalsIgnoreCase(value));
    }
}
