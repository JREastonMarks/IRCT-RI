-- Set the resource parameters
set @resourceName = '{{i2b2resourcename.msg}}';
set @resourceURL = '{{i2b2resourceurl.msg}}';
set @domain = '{{i2b2domain.msg}}';
set @resourceImplementingInterface = 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2XMLResourceImplementation';
set @resourceOntology = 'TREE';
set @proxyURL = '{{i2b2proxyURL.msg}}';

-- Set the resource variables
set @resourceId = (select IF(id is NULL,0,max(id)) from Resource) + 1;

-- Set the resource predicates
set @containsId = (select IF(id is NULL,0,max(id)) from PredicateType) + 1;
set @constrainModifierId = @containsId + 1;
set @constrainValueId = @containsId + 2;
set @constrainDateId = @containsId + 3;

-- Set the Fields
set @modifier_FieldId = (select IF(id is NULL,0,max(id)) from Field) + 1;
set @operator_ConstrainValueId = @modifier_FieldId + 1;
set @constraint_ConstrainValueId = @modifier_FieldId + 2;
set @unitOfMeasure_ConstrainValueId = @modifier_FieldId + 3;
set @fromInclusive_ConstrainDateId = @modifier_FieldId + 4;
set @fromTime_ConstrainDateId = @modifier_FieldId + 5;
set @fromDate_ConstrainDateId = @modifier_FieldId + 6;
set @toInclusive_ConstrainDateId = @modifier_FieldId + 7;
set @toTime_ConstrainDateId = @modifier_FieldId + 8;
set @toDate_ConstrainDateId = @modifier_FieldId + 9;

-- INSERT THE RESOURCE
insert into Resource(id, implementingInterface, name, ontologyType) values(@resourceId, @resourceImplementingInterface, @resourceName, @resourceOntology);

-- INSERT THE RESOURCE PARAMERTERS
insert into resource_parameters(id, value, name) values(@resourceId, 'resourceName', @resourceName);
insert into resource_parameters(id, value, name) values(@resourceId, 'resourceURL', @resourceURL);
insert into resource_parameters(id, value, name) values(@resourceId, 'domain', @domain);
insert into resource_parameters(id, value, name) values(@resourceId, 'proxyURL', @proxyURL);

-- INSERT RESOURCE DATATYEPS
insert into Resource_dataTypes(Resource_id, datatypes) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:DATETIME');
insert into Resource_dataTypes(Resource_id, datatypes) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:DATE');
insert into Resource_dataTypes(Resource_id, datatypes) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:INTEGER');
insert into Resource_dataTypes(Resource_id, datatypes) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:STRING');
insert into Resource_dataTypes(Resource_id, datatypes) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:FLOAT');

-- INSERT RESOURCE RELATIONSHIPS
insert into Resource_relationships(Resource_id, relationships) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2OntologyRelationship:PARENT');
insert into Resource_relationships(Resource_id, relationships) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2OntologyRelationship:CHILD');
insert into Resource_relationships(Resource_id, relationships) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2OntologyRelationship:SIBLING');
insert into Resource_relationships(Resource_id, relationships) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2OntologyRelationship:MODIFIER');
insert into Resource_relationships(Resource_id, relationships) values(@resourceId, 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2OntologyRelationship:TERM');

-- INSERT RESOURCE LogicalOperators
insert into Resource_logicalOperator(id, logicalOperator) values(@resourceId, 'AND');
insert into Resource_logicalOperator(id, logicalOperator) values(@resourceId, 'OR');
insert into Resource_logicalOperator(id, logicalOperator) values(@resourceId, 'NOT');

-- CONTAINS PREDICATE
insert into PredicateType(id, defaultPredicate, description, displayName, name) values(@containsId, 1, 'Contains value', 'Contains', 'CONTAINS');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@contains_FieldId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:STRING');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@contains_FieldId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:INTEGER');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@contains_FieldId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:FLOAT');

insert into Resource_PredicateType(Resource_Id, supportedPredicates_id) values(@resourceId, @containsId);

-- CONSTRAIN BY MODIFER
insert into PredicateType(id, defaultPredicate, description, displayName, name) values(@constrainModifierId, 0, 'Constrain by Modifier', 'Constrain by Modifier', 'CONSTRAIN_MODIFIER');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@constrainModifierId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:STRING');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@constrainModifierId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:INTEGER');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@constrainModifierId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:FLOAT');

insert into Field(id, description, name, path, relationship, required) values(@modifier_FieldId, 'Constrain by a modifier of this entity', 'Modifier', 'MODIFIER_KEY', 'edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2OntologyRelationship:MODIFIER', 1);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainModifierId, @modifier_FieldId);

insert into Resource_PredicateType(Resource_Id, supportedPredicates_id) values(@resourceId, @constrainModifierId);

-- CONSTRAIN BY VALUE
insert into PredicateType(id, defaultPredicate, description, displayName, name) values(@constrainValueId, 0, 'Constrains by Value', 'Constrain by Value', 'CONSTRAIN_VALUE');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@constrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:STRING');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@constrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:INTEGER');
insert into PredicateType_dataTypes(PredicateType_id, dataTypes) values(@constrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:FLOAT');

insert into Field(id, description, name, path, relationship, required) values(@operator_ConstrainValueId, 'Operator', 'Operator', 'OPERATOR', null, 1);
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'EQ');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'NE');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'GT');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'GE');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'LT');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'LE');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'BETWEEN');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'LIKE[exact]');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'LIKE[begin]');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'LIKE[end]');
insert into Field_permittedValues(Field_Id, permittedValues) values(@operator_ConstrainValueId, 'LIKE[contains]');

insert into Field(id, description, name, path, relationship, required) values(@constraint_ConstrainValueId, 'Constraint', 'Constraint', 'CONSTRAINT', null, 1);
insert into Field_dataTypes(Field_id, dataTypes) values(@constraint_ConstrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:STRING');
insert into Field_dataTypes(Field_id, dataTypes) values(@constraint_ConstrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:INTEGER');
insert into Field_dataTypes(Field_id, dataTypes) values(@constraint_ConstrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:FLOAT');

insert into Field(id, description, name, path, relationship, required) values(@unitOfMeasure_ConstrainValueId, 'Unit of Measure', 'Unit of Measure', 'UNIT_OF_MEASURE', null, 0);
insert into Field_dataTypes(Field_id, dataTypes) values(@unitOfMeasure_ConstrainValueId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:STRING');

insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainValueId, @operator_ConstrainValueId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainValueId, @constraint_ConstrainValueId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainValueId, @unitOfMeasure_ConstrainValueId);


insert into Resource_PredicateType(Resource_Id, supportedPredicates_id) values(@resourceId, @constrainValueId);
-- CONSTRAIN BY DATE
insert into PredicateType(id, defaultPredicate, description, displayName, name) values(@constrainDateId, 0, 'Constrains by Date', 'Constrain by Date', 'CONSTRAIN_DATE');


insert into Field(id, description, name, path, relationship, required) values(@fromInclusive_ConstrainDateId, 'Inclusive From Date', 'From Inclusive', 'FROM_INCLUSIVE', null, 1);
insert into Field_permittedValues(Field_Id, permittedValues) values(@fromInclusive_ConstrainDateId, 'YES');
insert into Field_permittedValues(Field_Id, permittedValues) values(@fromInclusive_ConstrainDateId, 'NO');


insert into Field(id, description, name, path, relationship, required) values(@fromTime_ConstrainDateId, 'From Date Start or End', 'From Time', 'FROM_TIME', null, 1);
insert into Field_permittedValues(Field_Id, permittedValues) values(@fromTime_ConstrainDateId, 'START_DATE');
insert into Field_permittedValues(Field_Id, permittedValues) values(@fromTime_ConstrainDateId, 'END_DATE');


insert into Field(id, description, name, path, relationship, required) values(@fromDate_ConstrainDateId, 'From Date', 'From Date', 'FROM_DATE', null, 1);
insert into Field_dataTypes(Field_id, dataTypes) values(@fromDate_ConstrainDateId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:DATE');

insert into Field(id, description, name, path, relationship, required) values(@toInclusive_ConstrainDateId, 'Inclusive To Date', 'To Inclusive', 'TO_INCLUSIVE', null, 1);
insert into Field_permittedValues(Field_Id, permittedValues) values(@toInclusive_ConstrainDateId, 'YES');
insert into Field_permittedValues(Field_Id, permittedValues) values(@toInclusive_ConstrainDateId, 'NO');

insert into Field(id, description, name, path, relationship, required) values(@toTime_ConstrainDateId, 'To Date Start or End', 'To Time', 'TO_TIME', null, 1);
insert into Field_permittedValues(Field_Id, permittedValues) values(@toTime_ConstrainDateId, 'START_DATE');
insert into Field_permittedValues(Field_Id, permittedValues) values(@toTime_ConstrainDateId, 'END_DATE');

insert into Field(id, description, name, path, relationship, required) values(@toDate_ConstrainDateId, 'To Date', 'To Date', 'TO_DATE', null, 1);
insert into Field_dataTypes(Field_id, dataTypes) values(@toDate_ConstrainDateId, 'edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType:DATE');


insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainDateId, @fromInclusive_ConstrainDateId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainDateId, @fromTime_ConstrainDateId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainDateId, @fromDate_ConstrainDateId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainDateId, @toInclusive_ConstrainDateId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainDateId, @toTime_ConstrainDateId);
insert into PredicateType_Field(PredicateType_id, fields_id) values(@constrainDateId, @toDate_ConstrainDateId);

insert into Resource_PredicateType(Resource_Id, supportedPredicates_id) values(@resourceId, @constrainDateId);
