package org.example;

import java.util.Optional;

public class Main {
    public static void main(String[] args) {

        PipedRunnableCompute computation = new PipedRunnableCompute();

        for (int i = 0; i < 3; i++) {
            System.out.printf("Run #%d:\n", i + 1);
            Optional<Integer> result = computation.computeFunctions(i);
            if (result.isPresent()) {
                System.out.printf("Computed result: %d\n", result.get());
            } else {
                System.out.printf("Canceled by user. Description: %s", computation.errorDescription);
            }
            System.out.print(computation.computationTimeTaken);
        }
    }
}