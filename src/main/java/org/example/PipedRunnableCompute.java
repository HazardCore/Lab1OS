package org.example;


import os.lab1.compfuncs.basic.IntOps;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Scanner;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;

public class PipedRunnableCompute {
    private static Runnable createRunnableFromFuncF(int i, PipedOutputStream out) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> result = IntOps.trialF(i);
                    putInt(out, result);

                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    } else {
                        //e.printStackTrace();
                    }
                }
            }
        };
    }

    private static Runnable createRunnableFromFuncG(int i, PipedOutputStream out) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> result = IntOps.trialG(i);
                    putInt(out, result);

                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    } else {
                        //e.printStackTrace();
                    }
                }
            }
        };
    }

    static void putInt(PipedOutputStream output, Optional<Integer> i) throws IOException {
        ByteBuffer result = ByteBuffer.allocate(8);
        result.putInt(i.get());
        output.write(result.array(), 0, 8);
    }

    static int getInt(PipedInputStream input) throws IOException {
        if (input.available() < 8)
            throw new IOException("No bytes");
        byte[] bytes = new byte[8];

        int i = input.read(bytes, 0, 8);
        ByteBuffer resBuffer = ByteBuffer.wrap(bytes);
        return resBuffer.getInt();
    }

    public String errorDescription;
    public String computationTimeTaken;

    public Optional<Integer> computeFunctions(int arg1) {

        errorDescription = "";
        computationTimeTaken = "";

        PipedInputStream[] inputStreams = new PipedInputStream[] {
                new PipedInputStream(), new PipedInputStream()
        };
        PipedOutputStream[] outputStreams = new PipedOutputStream[] {
                new PipedOutputStream(), new PipedOutputStream()
        };


        try {
            for (int i = 0; i < 2; i++) {
                inputStreams[i].connect(outputStreams[i]);
            }
        } catch (IOException e) {
            System.out.println("Error");
            return Optional.empty();
        }
        Runnable fR = createRunnableFromFuncF(arg1, outputStreams[0]);
        Runnable gR = createRunnableFromFuncG(arg1, outputStreams[1]);

        Thread[] threads = {
                new Thread(fR),
                new Thread(gR)
        };

        for (int i = 0; i < 2; i++)
            threads[i].start();

        ArrayList<Optional<Integer>> vals = new ArrayList<>();
        vals.add(Optional.empty());
        vals.add(Optional.empty());

        Optional<Integer> result = Optional.empty();
        try {
            long timerStart = System.nanoTime(), timer = 0;
            long computationStart = System.nanoTime();

            boolean unlimitedRunFlag = true;
            boolean receivedResultFlag = false;
            boolean terminateFlag = false;

            while (!receivedResultFlag) {
                for (int i = 0; i < 2; i++) {
                    if (!vals.get(i).isPresent() && inputStreams[i].available() >= 8) {
                        vals.set(i, Optional.of(getInt(inputStreams[i])));

                        if (vals.get(i).get().equals(BinaryOperation.getZeroValue())) {
                            result = Optional.of(BinaryOperation.getZeroResult());
                            receivedResultFlag = true;
                            break;
                        } else if (vals.get(0).isPresent() && vals.get(1).isPresent()) {
                            result = Optional.of(BinaryOperation.calculate(vals.get(0).get(), vals.get(1).get()));
                            receivedResultFlag = true;
                            break;
                        }
                    }
                }

                if (terminateFlag || receivedResultFlag)
                    break;

                Thread.sleep(30);

                timer = System.nanoTime() - timerStart;

                if (unlimitedRunFlag && timer / 1_000_000_000 >= 10) {
                    System.out.println("Continue (C), Continue Without prompt (N), Terminate (T):");
                    String userInput;

                    do {
                        Scanner input = new Scanner(System.in);
                        userInput = input.next().toLowerCase();
                    } while (userInput.equals("C") && userInput.equals("N") && userInput.equals("T"));

                    // For C do nothing

                    if (userInput.equals("N")) {
                        unlimitedRunFlag = false;
                    } else if (userInput.equals("T")) {
                        terminateFlag = true;
                    }
                    timer = System.nanoTime();
                }

            }

            for (int i = 0; i < 2; i++) {
                threads[i].interrupt();
                inputStreams[i].close();
                outputStreams[i].close();
            }

            if (terminateFlag && !result.isPresent()) {
                if (!vals.get(0).isPresent() && !vals.get(1).isPresent()) {
                    errorDescription += "F and G functions value is unknown";
                }
                else if (!vals.get(0).isPresent()) {
                    errorDescription += "F function value is unknown";
                }
                else if (!vals.get(1).isPresent()) {
                    errorDescription += "G function value is unknown";
                }
            }

            computationTimeTaken = String.format("\nComputation took %f s\n", (double)(System.nanoTime() - computationStart) / 1_000_000_000);

            return result;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
