package com.onlineexam.task3;

import java.math.*;
import java.util.*;

public final class Grading {
    private Grading() {}
    public static BigDecimal score(String type, List<String> answer, List<String> standard, BigDecimal max) {
        if (!Set.of("SINGLE_CHOICE","MULTIPLE_CHOICE","JUDGE").contains(type))
            throw new BusinessException(409,"不支持的题型");
        return (!answer.isEmpty() && new HashSet<>(answer).equals(new HashSet<>(standard))
            ? max : BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
    }
}
