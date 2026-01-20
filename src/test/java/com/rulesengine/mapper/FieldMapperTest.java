package com.rulesengine.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.exception.FieldMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FieldMapperTest {

    private FieldMapper fieldMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fieldMapper = new FieldMapper(objectMapper);
    }

    @Test
    void extractValue_SimpleProperty_Success() {
        // Given
        TestObject obj = new TestObject("John", 30);
        
        // When
        Object result = fieldMapper.extractValue(obj, "name");
        
        // Then
        assertEquals("John", result);
    }

    @Test
    void extractValue_NestedProperty_Success() {
        // Given
        TestObject inner = new TestObject("Jane", 25);
        TestObjectWithNested obj = new TestObjectWithNested(inner, "outer");
        
        // When
        Object result = fieldMapper.extractValue(obj, "nested.name");
        
        // Then
        assertEquals("Jane", result);
    }

    @Test
    void extractValue_ArrayIndexing_Success() {
        // Given
        List<String> items = Arrays.asList("first", "second", "third");
        TestObjectWithArray obj = new TestObjectWithArray(items);
        
        // When
        Object result = fieldMapper.extractValue(obj, "items[1]");
        
        // Then
        assertEquals("second", result);
    }

    @Test
    void extractValue_ArrayIndexOutOfBounds_ThrowsException() {
        // Given
        List<String> items = Arrays.asList("first", "second");
        TestObjectWithArray obj = new TestObjectWithArray(items);
        
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.extractValue(obj, "items[5]"));
        
        assertTrue(exception.getMessage().contains("Array index 5 out of bounds"));
        assertEquals("items[5]", exception.getMapperExpression());
    }

    @Test
    void extractValue_ArrayFiltering_Success() {
        // Given
        List<TestObject> users = Arrays.asList(
            new TestObject("John", 30),
            new TestObject("Jane", 25),
            new TestObject("Bob", 35)
        );
        TestObjectWithUsers obj = new TestObjectWithUsers(users);
        
        // When
        Object result = fieldMapper.extractValue(obj, "users[name=Jane].age");
        
        // Then
        assertEquals(25, result);
    }

    @Test
    void extractValue_ArrayFilteringNotFound_ThrowsException() {
        // Given
        List<TestObject> users = Arrays.asList(
            new TestObject("John", 30),
            new TestObject("Jane", 25)
        );
        TestObjectWithUsers obj = new TestObjectWithUsers(users);
        
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.extractValue(obj, "users[name=Alice].age"));
        
        assertTrue(exception.getMessage().contains("No array item found with name=Alice"));
    }

    @Test
    void extractValue_MapAccess_Success() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 42);
        
        // When
        Object result = fieldMapper.extractValue(data, "key2");
        
        // Then
        assertEquals(42, result);
    }

    @Test
    void extractValue_JsonNodeAccess_Success() throws Exception {
        // Given
        String json = "{\"user\": {\"name\": \"John\", \"age\": 30}}";
        JsonNode jsonNode = objectMapper.readTree(json);
        
        // When
        Object result = fieldMapper.extractValue(jsonNode, "user.name");
        
        // Then
        assertEquals("John", result);
    }

    @Test
    void extractValue_NullResponse_ThrowsException() {
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.extractValue(null, "name"));
        
        assertTrue(exception.getMessage().contains("Cannot extract value from null response"));
    }

    @Test
    void extractValue_EmptyExpression_ThrowsException() {
        // Given
        TestObject obj = new TestObject("John", 30);
        
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.extractValue(obj, ""));
        
        assertTrue(exception.getMessage().contains("Mapper expression cannot be null or empty"));
    }

    @Test
    void extractValue_PropertyNotFound_ThrowsException() {
        // Given
        TestObject obj = new TestObject("John", 30);
        
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.extractValue(obj, "nonexistent"));
        
        assertTrue(exception.getMessage().contains("Property 'nonexistent' not found"));
        assertEquals("nonexistent", exception.getMapperExpression());
    }

    @Test
    void convertType_StringToInteger_Success() {
        // When
        Object result = fieldMapper.convertType("123", Integer.class);
        
        // Then
        assertEquals(123, result);
    }

    @Test
    void convertType_StringToDouble_Success() {
        // When
        Object result = fieldMapper.convertType("123.45", Double.class);
        
        // Then
        assertEquals(123.45, result);
    }

    @Test
    void convertType_StringToBoolean_Success() {
        // When
        Object result1 = fieldMapper.convertType("true", Boolean.class);
        Object result2 = fieldMapper.convertType("false", Boolean.class);
        Object result3 = fieldMapper.convertType("1", Boolean.class);
        Object result4 = fieldMapper.convertType("0", Boolean.class);
        
        // Then
        assertTrue((Boolean) result1);
        assertFalse((Boolean) result2);
        assertTrue((Boolean) result3);
        assertFalse((Boolean) result4);
    }

    @Test
    void convertType_StringToLocalDate_Success() {
        // When
        Object result = fieldMapper.convertType("2023-12-25", LocalDate.class);
        
        // Then
        assertEquals(LocalDate.of(2023, 12, 25), result);
    }

    @Test
    void convertType_StringToLocalDateTime_Success() {
        // When
        Object result = fieldMapper.convertType("2023-12-25T10:30:00", LocalDateTime.class);
        
        // Then
        assertEquals(LocalDateTime.of(2023, 12, 25, 10, 30, 0), result);
    }

    @Test
    void convertType_StringToBigDecimal_Success() {
        // When
        Object result = fieldMapper.convertType("123.456789", BigDecimal.class);
        
        // Then
        assertEquals(new BigDecimal("123.456789"), result);
    }

    @Test
    void convertType_InvalidStringToInteger_ThrowsException() {
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.convertType("not_a_number", Integer.class));
        
        assertTrue(exception.getMessage().contains("Failed to convert"));
    }

    @Test
    void convertType_InvalidStringToBoolean_ThrowsException() {
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.convertType("maybe", Boolean.class));
        
        assertTrue(exception.getMessage().contains("Failed to convert"));
    }

    @Test
    void convertType_InvalidStringToDate_ThrowsException() {
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class, 
            () -> fieldMapper.convertType("not_a_date", LocalDate.class));
        
        assertTrue(exception.getMessage().contains("Failed to convert"));
    }

    @Test
    void convertType_NullValue_ReturnsNull() {
        // When
        Object result = fieldMapper.convertType(null, String.class);
        
        // Then
        assertNull(result);
    }

    @Test
    void convertType_SameType_ReturnsOriginal() {
        // Given
        String value = "test";
        
        // When
        Object result = fieldMapper.convertType(value, String.class);
        
        // Then
        assertSame(value, result);
    }

    @Test
    void extractValue_ComplexNestedStructure_Success() {
        // Given
        Map<String, Object> address = new HashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "Springfield");
        
        Map<String, Object> user = new HashMap<>();
        user.put("name", "John");
        user.put("address", address);
        
        List<Map<String, Object>> users = Arrays.asList(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        
        // When
        Object result = fieldMapper.extractValue(response, "users[0].address.city");
        
        // Then
        assertEquals("Springfield", result);
    }

    @Test
    void extractValue_ArrayIteration_Success() {
        // Given
        List<String> items = Arrays.asList("a", "b", "c");
        TestObjectWithArray obj = new TestObjectWithArray(items);
        
        // When
        Object result = fieldMapper.extractValue(obj, "items[*]");
        
        // Then
        assertTrue(result instanceof List);
        List<?> resultList = (List<?>) result;
        assertEquals(3, resultList.size());
        assertEquals("a", resultList.get(0));
    }

    // Test helper classes
    public static class TestObject {
        private String name;
        private int age;
        
        public TestObject(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
    }
    
    public static class TestObjectWithNested {
        private TestObject nested;
        private String value;
        
        public TestObjectWithNested(TestObject nested, String value) {
            this.nested = nested;
            this.value = value;
        }
        
        public TestObject getNested() { return nested; }
        public String getValue() { return value; }
    }
    
    public static class TestObjectWithArray {
        private List<String> items;
        
        public TestObjectWithArray(List<String> items) {
            this.items = items;
        }
        
        public List<String> getItems() { return items; }
    }
    
    public static class TestObjectWithUsers {
        private List<TestObject> users;
        
        public TestObjectWithUsers(List<TestObject> users) {
            this.users = users;
        }
        
        public List<TestObject> getUsers() { return users; }
    }
}