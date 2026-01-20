package com.rulesengine.calculator.builtin;

import com.rulesengine.calculator.AbstractFieldCalculator;
import com.rulesengine.calculator.CalculatorException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Calculator that computes the difference between two date fields
 */
@Component
public class DateDiffCalculator extends AbstractFieldCalculator {
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };
    
    @Override
    public Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException {
        String startDateField = getRequiredParameter(parameters, "startDateField", String.class);
        String endDateField = getRequiredParameter(parameters, "endDateField", String.class);
        String unit = getRequiredParameter(parameters, "unit", String.class);
        
        Object startDateValue = context.get(startDateField);
        Object endDateValue = context.get(endDateField);
        
        if (startDateValue == null) {
            throw new CalculatorException("Start date field '" + startDateField + "' is null", getName(), startDateField);
        }
        
        if (endDateValue == null) {
            throw new CalculatorException("End date field '" + endDateField + "' is null", getName(), endDateField);
        }
        
        LocalDateTime startDateTime = parseDate(startDateValue, startDateField);
        LocalDateTime endDateTime = parseDate(endDateValue, endDateField);
        ChronoUnit chronoUnit = parseUnit(unit);
        
        return chronoUnit.between(startDateTime, endDateTime);
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        getRequiredParameter(parameters, "startDateField", String.class);
        getRequiredParameter(parameters, "endDateField", String.class);
        String unit = getRequiredParameter(parameters, "unit", String.class);
        
        parseUnit(unit); // Validate unit
    }
    
    private LocalDateTime parseDate(Object dateValue, String fieldName) throws CalculatorException {
        if (dateValue instanceof LocalDateTime) {
            return (LocalDateTime) dateValue;
        }
        
        if (dateValue instanceof LocalDate) {
            return ((LocalDate) dateValue).atStartOfDay();
        }
        
        if (dateValue instanceof String) {
            String dateStr = (String) dateValue;
            
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    if (dateStr.contains("T") || dateStr.contains(" ")) {
                        return LocalDateTime.parse(dateStr, formatter);
                    } else {
                        return LocalDate.parse(dateStr, formatter).atStartOfDay();
                    }
                } catch (DateTimeParseException ignored) {
                    // Try next formatter
                }
            }
            
            throw new CalculatorException("Unable to parse date: " + dateStr, getName(), fieldName);
        }
        
        throw new CalculatorException("Unsupported date type: " + dateValue.getClass().getSimpleName(), 
                getName(), fieldName);
    }
    
    private ChronoUnit parseUnit(String unit) throws CalculatorException {
        try {
            switch (unit.toLowerCase()) {
                case "days":
                case "day":
                    return ChronoUnit.DAYS;
                case "weeks":
                case "week":
                    return ChronoUnit.WEEKS;
                case "months":
                case "month":
                    return ChronoUnit.MONTHS;
                case "years":
                case "year":
                    return ChronoUnit.YEARS;
                case "hours":
                case "hour":
                    return ChronoUnit.HOURS;
                case "minutes":
                case "minute":
                    return ChronoUnit.MINUTES;
                case "seconds":
                case "second":
                    return ChronoUnit.SECONDS;
                default:
                    throw new CalculatorException("Unsupported time unit: " + unit, getName(), null);
            }
        } catch (Exception e) {
            throw new CalculatorException("Invalid time unit: " + unit, e, getName(), null);
        }
    }
    
    @Override
    public String getName() {
        return "date_diff";
    }
    
    @Override
    public String getDescription() {
        return "Calculates the difference between two date fields in specified time units";
    }
}