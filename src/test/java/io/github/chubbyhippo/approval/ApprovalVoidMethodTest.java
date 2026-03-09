package io.github.chubbyhippo.approval;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class ApprovalVoidMethodTest {

    @Test
    void testVoidMethodOutput() {
        var outputStream = new ByteArrayOutputStream();
        var originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            printHello();
            Approvals.verify(outputStream.toString());
        } finally {
            System.setOut(originalOut);
        }
    }

    private void printHello() {
        System.out.println("Hello, Approval Tests!");
    }
}
