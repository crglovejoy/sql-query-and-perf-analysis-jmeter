import java.math.BigDecimal

if (args.length != 3) {
    throw new IllegalArgumentException("Caller provided " + args.length.toString() + " arguments but expected 3")
}

// The prefixes of the variables we need to retrieve
String varPrefixNumExec = args[0]
String varPrefixTotElapsed = args[1]

/* Our threshold for determining a pass/fail on the test.  If our average query elapsed time is equal to or exceeds this value,
 * we should fail the assertion */
BigDecimal thresholdMs = args[2] as BigDecimal

/* Validate that we got 1 row of data to analyze.  Checking the number of instances of the exec_col and assuming the number of
 * instances of the tot_elapsed_col will be the same */  
String rowCountStr = vars.get(varPrefixNumExec + "_#")

if (rowCountStr == null) {
	AssertionResult.setFailure(true)
	AssertionResult.setFailureMessage("Did not find a SQL query result to analyze")
	return
}

int rowCount = rowCountStr as int

if (rowCount == 0) {
	AssertionResult.setFailure(true)
	AssertionResult.setFailureMessage("Found 0 rows returned for analysis. Expecting 1 or more rows.")
	return
}

/* Consolidate the stats here...
 * Its possible to get multiple rows of stats for the same query.  Example: If different integer sizes are used as query parameters,
 * these may get interpreted by SQL as different integer types (tinyint, smallint, mediumint, etc.) and result in multiple
 * query plans and stats to analyze.  Consolidate all the stats into one set and then analyze the single set of stats.
 */

BigDecimal numExec = 0.0G
BigDecimal totElapsed = 0.0G

for(int i = 1; i < rowCount + 1; i++) {
    String numExecStr = vars.get(varPrefixNumExec + "_" + i.toString())
    String elapsedTimeStr = vars.get(varPrefixTotElapsed + "_" + i.toString())
    log.info("Found Number Executed = $numExecStr and Elapsed Time Microseconds = $elapsedTimeStr")

    // Consolidate the stats here...
    numExec += numExecStr as BigDecimal
    totElapsed += elapsedTimeStr as BigDecimal
}


// Get the average time elapsed, converting microseconds to milliseconds
BigDecimal avgElapsedMs = (totElapsed / numExec) / 1000.0G

// Analyze our result and take the appropriate action
if (avgElapsedMs.isAtLeast(thresholdMs)) {
    String errMsg = "Failure! Over " + numExec.toString() + " query executions an average elapsed execution time of " + avgElapsedMs.toString() + " ms is equal to or greater than the failure threshold of " + thresholdMs.toString() + " ms"
    AssertionResult.setFailure(true)
    AssertionResult.setFailureMessage(errMsg)
    log.error(errMsg)
} else {
    log.info("Success! Over " + numExec.toString() + " query executions an average elapsed execution time of " + avgElapsedMs.toString() + " ms is less than the failure threshold of " + thresholdMs.toString() + " ms")
}

