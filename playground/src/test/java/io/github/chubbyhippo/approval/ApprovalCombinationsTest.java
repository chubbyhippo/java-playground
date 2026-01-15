package io.github.chubbyhippo.approval;

import org.approvaltests.combinations.CombinationApprovals;
import org.junit.jupiter.api.Test;

public class ApprovalCombinationsTest {

    @Test
    void testCombinations() {
        String[] inputs1 = {"A", "B"};
        Integer[] inputs2 = {1, 2, 3};
        Boolean[] inputs3 = {true, false};

        CombinationApprovals.verifyAllCombinations(
                this::doSomething,
                inputs1,
                inputs2,
                inputs3
        );
    }

    private String doSomething(String s, Integer i, Boolean b) {
        return String.format("Result: %s, %d, %b", s, i, b);
    }
}