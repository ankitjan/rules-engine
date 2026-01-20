package com.rulesengine.calculator.builtin;

import com.rulesengine.calculator.CalculatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DateCalculatorTest {

    private DateAddCalculator dateAddCalculator;
    private DateDiffCalculator dateDiffCalculator;

    @BeforeEach
    void setUp() {
        dateAddCalculator = new DateAddCalculator();
        dateDiffCalculator = new DateDiffCalculator();
    }

    @Test
    void testDateAdd_WithLocalDate() throws CalculatorException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dateField", "startDate");
        parameters.put("unit", "days");
        parameters.put("amount", 5);

        Map<String, Object> context = new HashMap<>();
        context.put("startDate", LocalDate.of(2023, 1, 1));

        Object result = dateAddCalculator.calculate(parameters, context);

        assertNotNull(result);
        assertTrue(result instanceof LocalDateTime);
        LocalDateTime resultDate = (LocalDateTime) result;
        assertEquals(LocalDate.of(2023, 1, 6), resultDate.toLocalDate());
    }

    @Test
    void testDateAdd_WithStringDate() throws CalculatorException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dateField", "startDate");
        parameters.put("unit", "months");
        parameters.put("amount", 2);

        Map<String, Object> context = new HashMap<>();
        context.put("startDate", "2023-01-15");

        Object result = dateAddCalculator.calculate(parameters, context);

        assertNotNull(result);
        assertTrue(result instanceof String);
        String resultStr = (String) result;
        assertTrue(resultStr.startsWith("2023-03-15"));
    }

    @Test
    void testDateAdd_InvalidUnit() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dateField", "startDate");
        parameters.put("unit", "invalid");
        parameters.put("amount", 1);

        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> dateAddCalculator.validateParameters(parameters));

        assertTrue(exception.getMessage().contains("Invalid time unit"));
    }

    @Test
    void testDateDiff_WithLocalDates() throws CalculatorException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDateField", "startDate");
        parameters.put("endDateField", "endDate");
        parameters.put("unit", "days");

        Map<String, Object> context = new HashMap<>();
        context.put("startDate", LocalDate.of(2023, 1, 1));
        context.put("endDate", LocalDate.of(2023, 1, 11));

        Object result = dateDiffCalculator.calculate(parameters, context);

        assertNotNull(result);
        assertEquals(10L, result); // 10 days difference
    }

    @Test
    void testDateDiff_WithStringDates() throws CalculatorException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDateField", "startDate");
        parameters.put("endDateField", "endDate");
        parameters.put("unit", "weeks");

        Map<String, Object> context = new HashMap<>();
        context.put("startDate", "2023-01-01");
        context.put("endDate", "2023-01-15");

        Object result = dateDiffCalculator.calculate(parameters, context);

        assertNotNull(result);
        assertEquals(2L, result); // 2 weeks difference
    }

    @Test
    void testDateDiff_NullStartDate() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDateField", "startDate");
        parameters.put("endDateField", "endDate");
        parameters.put("unit", "days");

        Map<String, Object> context = new HashMap<>();
        context.put("startDate", null);
        context.put("endDate", LocalDate.of(2023, 1, 11));

        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> dateDiffCalculator.calculate(parameters, context));

        assertTrue(exception.getMessage().contains("Start date field 'startDate' is null"));
    }

    @Test
    void testDateAdd_InvalidDateFormat() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dateField", "startDate");
        parameters.put("unit", "days");
        parameters.put("amount", 1);

        Map<String, Object> context = new HashMap<>();
        context.put("startDate", "invalid-date");

        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> dateAddCalculator.calculate(parameters, context));

        assertTrue(exception.getMessage().contains("Unable to parse date"));
    }

    @Test
    void testDateCalculators_ValidateParameters() throws CalculatorException {
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("dateField", "date");
        addParams.put("unit", "days");
        addParams.put("amount", 1);

        assertDoesNotThrow(() -> dateAddCalculator.validateParameters(addParams));

        Map<String, Object> diffParams = new HashMap<>();
        diffParams.put("startDateField", "start");
        diffParams.put("endDateField", "end");
        diffParams.put("unit", "days");

        assertDoesNotThrow(() -> dateDiffCalculator.validateParameters(diffParams));
    }
}