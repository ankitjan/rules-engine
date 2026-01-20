-- Add inheritance and metadata support to entity_types table
ALTER TABLE entity_types ADD COLUMN parent_type_name VARCHAR(255);
ALTER TABLE entity_types ADD COLUMN metadata TEXT;
ALTER TABLE entity_types ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add foreign key constraint for parent type (self-referencing)
ALTER TABLE entity_types ADD CONSTRAINT fk_entity_types_parent 
    FOREIGN KEY (parent_type_name) REFERENCES entity_types(type_name) ON DELETE SET NULL;

-- Add index for parent type lookups
CREATE INDEX idx_entity_types_parent_type_name ON entity_types(parent_type_name);
CREATE INDEX idx_entity_types_is_active ON entity_types(is_active);