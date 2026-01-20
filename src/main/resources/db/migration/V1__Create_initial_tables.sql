-- Create users table (already exists but included for completeness)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create folders table for rule organization
CREATE TABLE folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    parent_id BIGINT,
    path VARCHAR(2000), -- Materialized path for efficient hierarchy queries
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Create rules table
CREATE TABLE rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    rule_definition TEXT NOT NULL, -- JSON representation of the rule
    folder_id BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Create field_configs table
CREATE TABLE field_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name VARCHAR(255) NOT NULL UNIQUE,
    field_type VARCHAR(50) NOT NULL, -- STRING, NUMBER, DATE, BOOLEAN, etc.
    description VARCHAR(1000),
    data_service_config TEXT, -- JSON configuration for data service
    mapper_expression VARCHAR(500), -- Expression for mapping response to field value
    is_calculated BOOLEAN NOT NULL DEFAULT FALSE,
    calculator_config TEXT, -- JSON configuration for calculated fields
    dependencies TEXT, -- JSON array of field names this field depends on
    default_value VARCHAR(500),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255)
);

-- Create entity_types table
CREATE TABLE entity_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    data_service_config TEXT NOT NULL, -- JSON configuration for retrieving entities
    field_mappings TEXT, -- JSON mapping of entity fields to field configurations
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255)
);

-- Create rule_versions table for version history
CREATE TABLE rule_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    rule_definition TEXT NOT NULL,
    change_description VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    is_tagged BOOLEAN NOT NULL DEFAULT FALSE,
    tag_name VARCHAR(255),
    FOREIGN KEY (rule_id) REFERENCES rules(id) ON DELETE CASCADE,
    UNIQUE (rule_id, version_number)
);

-- Create audit_log table for tracking changes
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    operation VARCHAR(20) NOT NULL, -- INSERT, UPDATE, DELETE
    old_values TEXT, -- JSON representation of old values
    new_values TEXT, -- JSON representation of new values
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes separately for H2 compatibility
CREATE INDEX idx_folders_parent_id ON folders(parent_id);
CREATE INDEX idx_folders_path ON folders(path);
CREATE INDEX idx_folders_is_deleted ON folders(is_deleted);

CREATE INDEX idx_rules_folder_id ON rules(folder_id);
CREATE INDEX idx_rules_name ON rules(name);
CREATE INDEX idx_rules_is_active ON rules(is_active);
CREATE INDEX idx_rules_is_deleted ON rules(is_deleted);
CREATE INDEX idx_rules_created_at ON rules(created_at);

CREATE INDEX idx_field_configs_field_name ON field_configs(field_name);
CREATE INDEX idx_field_configs_field_type ON field_configs(field_type);
CREATE INDEX idx_field_configs_is_calculated ON field_configs(is_calculated);
CREATE INDEX idx_field_configs_is_deleted ON field_configs(is_deleted);

CREATE INDEX idx_entity_types_type_name ON entity_types(type_name);
CREATE INDEX idx_entity_types_is_deleted ON entity_types(is_deleted);

CREATE INDEX idx_rule_versions_rule_id ON rule_versions(rule_id);
CREATE INDEX idx_rule_versions_created_at ON rule_versions(created_at);

CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at);
CREATE INDEX idx_audit_log_changed_by ON audit_log(changed_by);