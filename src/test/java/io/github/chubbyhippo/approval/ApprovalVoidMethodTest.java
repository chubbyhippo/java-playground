package io.github.chubbyhippo.approval;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class ApprovalVoidMethodTest {

  @Test
  void testVoidMethodOutput() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
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
