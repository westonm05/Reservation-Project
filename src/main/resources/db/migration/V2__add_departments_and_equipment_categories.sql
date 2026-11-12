-- V2: two lookup tables, linked to existing tables with new foreign-key columns.
--
-- departments          - academic departments; a user belongs to one, a lab is owned by one
-- equipment_categories - kinds of equipment (laptop, 3D printer...) and their loan limit
--
-- The new foreign-key columns are nullable so rows created before this migration stay valid
-- and so self-registered students don't have to pick a department.

CREATE TABLE departments (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    code VARCHAR(10)  NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uq_departments_code UNIQUE (code),
    CONSTRAINT uq_departments_name UNIQUE (name)
);

CREATE TABLE equipment_categories (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(255),
    max_loan_hours INT          NOT NULL,
    CONSTRAINT pk_equipment_categories PRIMARY KEY (id),
    CONSTRAINT uq_equipment_categories_name UNIQUE (name),
    CONSTRAINT chk_equipment_categories_hours CHECK (max_loan_hours > 0)
);

ALTER TABLE users ADD COLUMN department_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_department
    FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE labs ADD COLUMN department_id BIGINT;
ALTER TABLE labs ADD CONSTRAINT fk_labs_department
    FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE equipment ADD COLUMN category_id BIGINT;
ALTER TABLE equipment ADD CONSTRAINT fk_equipment_category
    FOREIGN KEY (category_id) REFERENCES equipment_categories (id);
