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
 * Calculator that adds time units to a date field
 */
@Component
public class DateAddCalculator extends AbstractFieldCalculator {
    
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
        String dateField = getRequiredParameter(parameters, "dateField", String.class);
        String unit = getRequiredParameter(parameters, "unit", String.class);
        Number amount = getRequiredParameter(parameters, "amount", Number.class);
        
        Object dateValue = context.get(dateField);
        if (dateValue == null) {
            throw new CalculatorException("Date field '" + dateField + "' is null", getName(), dateField);
        }
        
        LocalDateTime dateTime = parseDate(dateValue, dateField);
        ChronoUnit chronoUnit = parseUnit(unit);
        
        LocalDateTime result = dateTime.plus(amount.longValue(), chronoUnit);
        
        // Return in the same format as input if possible
        if (dateValue instanceof String) {
            return result.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        return result;
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        getRequiredParameter(parameters, "dateField", String.class);
        String unit = getRequiredParameter(parameters, "unit", String.class);
        getRequiredParameter(parameters, "amount", Number.class);
        
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
        return "date_add";
    }
    
    @Override
    public String getDescription() {
        return "Adds a specified amount of time units to a date field";
    }
}