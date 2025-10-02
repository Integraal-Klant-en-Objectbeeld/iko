Alter table aggregated_data_profile add column connector_instance_id UUID;
alter table aggregated_data_profile add constraint aggregated_data_profile_connector_instance_id_fk foreign key (connector_instance_id) references connector_instance(id);

Alter table aggregated_data_profile add column connector_endpoint_id UUID;
alter table aggregated_data_profile add constraint aggregated_data_profile_connector_endpoint_id_fk foreign key (connector_endpoint_id) references connector_endpoint(id);

Alter table relation add column connector_instance_id UUID;
alter table relation add constraint relation_connector_instance_id_fk foreign key (connector_instance_id) references connector_instance(id);

Alter table relation add column connector_endpoint_id UUID;
alter table relation add constraint relation_connector_endpoint_id_fk foreign key (connector_endpoint_id) references connector_endpoint(id);
