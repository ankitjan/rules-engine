package com.rulesengine.dto;

import com.rulesengine.entity.FieldConfigEntity;

import java.util.*;

/**
 * Directed graph representing field dependencies and calculation order
 */
public class DependencyGraph {
    
    private Map<String, FieldConfigEntity> nodes;
    private Map<String, Set<String>> adjacencyList; // field -> set of fields it depends on
    private Map<String, Set<String>> reverseAdjacencyList; // field -> set of fields that depend on it
    
    public DependencyGraph() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.reverseAdjacencyList = new HashMap<>();
    }
    
    /**
     * Add a field node to the graph
     */
    public void addNode(FieldConfigEntity field) {
        if (field != null && field.getFieldName() != null) {
            nodes.put(field.getFieldName(), field);
            adjacencyList.putIfAbsent(field.getFieldName(), new HashSet<>());
            reverseAdjacencyList.putIfAbsent(field.getFieldName(), new HashSet<>());
        }
    }
    
    /**
     * Add a dependency edge from dependent field to dependency field
     * @param dependentField the field that depends on another
     * @param dependencyField the field that is depended upon
     */
    public void addDependency(String dependentField, String dependencyField) {
        if (dependentField != null && dependencyField != null) {
            // Add to adjacency list (dependent -> dependency)
            adjacencyList.computeIfAbsent(dependentField, k -> new HashSet<>()).add(dependencyField);
            
            // Add to reverse adjacency list (dependency -> dependent)
            reverseAdjacencyList.computeIfAbsent(dependencyField, k -> new HashSet<>()).add(dependentField);
            
            // Ensure both nodes exist in adjacency lists
            adjacencyList.putIfAbsent(dependencyField, new HashSet<>());
            reverseAdjacencyList.putIfAbsent(dependentField, new HashSet<>());
        }
    }
    
    /**
     * Get all dependencies for a field
     */
    public Set<String> getDependencies(String fieldName) {
        return adjacencyList.getOrDefault(fieldName, new HashSet<>());
    }
    
    /**
     * Get all fields that depend on this field
     */
    public Set<String> getDependents(String fieldName) {
        return reverseAdjacencyList.getOrDefault(fieldName, new HashSet<>());
    }
    
    /**
     * Get all field names in the graph
     */
    public Set<String> getAllFieldNames() {
        return new HashSet<>(nodes.keySet());
    }
    
    /**
     * Get field configuration by name
     */
    public FieldConfigEntity getField(String fieldName) {
        return nodes.get(fieldName);
    }
    
    /**
     * Check if the graph contains a field
     */
    public boolean containsField(String fieldName) {
        return nodes.containsKey(fieldName);
    }
    
    /**
     * Get fields with no dependencies (root nodes)
     */
    public Set<String> getRootFields() {
        return adjacencyList.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get fields with no dependents (leaf nodes)
     */
    public Set<String> getLeafFields() {
        return reverseAdjacencyList.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get the total number of nodes in the graph
     */
    public int getNodeCount() {
        return nodes.size();
    }
    
    /**
     * Get the total number of edges in the graph
     */
    public int getEdgeCount() {
        return adjacencyList.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
    
    /**
     * Check if the graph is empty
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }
    
    /**
     * Get a topological ordering of the fields (for dependency resolution)
     */
    public List<String> getTopologicalOrder() {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (String field : nodes.keySet()) {
            if (!visited.contains(field)) {
                topologicalSortUtil(field, visited, visiting, result);
            }
        }
        
        // No need to reverse - the DFS post-order gives us the correct topological order
        return result;
    }
    
    private void topologicalSortUtil(String field, Set<String> visited, Set<String> visiting, List<String> result) {
        if (visiting.contains(field)) {
            throw new IllegalStateException("Circular dependency detected involving field: " + field);
        }
        
        if (visited.contains(field)) {
            return;
        }
        
        visiting.add(field);
        
        // Visit all dependencies first
        for (String dependency : getDependencies(field)) {
            topologicalSortUtil(dependency, visited, visiting, result);
        }
        
        visiting.remove(field);
        visited.add(field);
        // Add current field after all its dependencies have been processed
        result.add(field);
    }
    
    /**
     * Detect circular dependencies in the graph
     */
    public List<String> detectCircularDependencies() {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<String> cycle = new ArrayList<>();
        
        for (String field : nodes.keySet()) {
            if (!visited.contains(field)) {
                if (hasCycleUtil(field, visited, visiting, cycle)) {
                    return cycle;
                }
            }
        }
        
        return Collections.emptyList(); // No cycles found
    }
    
    private boolean hasCycleUtil(String field, Set<String> visited, Set<String> visiting, List<String> cycle) {
        if (visiting.contains(field)) {
            // Found a cycle, build the cycle path
            cycle.add(field);
            return true;
        }
        
        if (visited.contains(field)) {
            return false;
        }
        
        visiting.add(field);
        
        for (String dependency : getDependencies(field)) {
            if (hasCycleUtil(dependency, visited, visiting, cycle)) {
                if (cycle.get(0).equals(field)) {
                    // Complete cycle found
                    return true;
                } else {
                    // Still building cycle path
                    cycle.add(field);
                    return true;
                }
            }
        }
        
        visiting.remove(field);
        visited.add(field);
        return false;
    }
    
    @Override
    public String toString() {
        return "DependencyGraph{" +
                "nodeCount=" + getNodeCount() +
                ", edgeCount=" + getEdgeCount() +
                ", rootFields=" + getRootFields() +
                ", leafFields=" + getLeafFields() +
                '}';
    }
}