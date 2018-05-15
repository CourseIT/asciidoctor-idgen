package com.amphiphile;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class IdGenerator {

    private static SecureRandom random = new SecureRandom();

    synchronized String generateId(int length) {

        long longId = Math.abs(random.nextLong());
        String longResult = Long.toString(longId, 32);
        List<String> lst = Arrays.asList(longResult.split(""));
        List<String> copy = new LinkedList<>(lst);
        Collections.shuffle(copy);
        return String.join("", copy.subList(0, length));
    }
}