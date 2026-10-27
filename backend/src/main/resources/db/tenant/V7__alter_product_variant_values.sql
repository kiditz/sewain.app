-- 1. Hapus constraint foreign key yang lama (komposit)
ALTER TABLE product_variant_values
DROP CONSTRAINT fk_variant_values_attribute_value;

-- 2. Tambahkan constraint foreign key baru yang hanya merujuk ke id di attribute_values
ALTER TABLE product_variant_values
    ADD CONSTRAINT fk_variant_values_attribute_value
        FOREIGN KEY (attribute_value_id)
            REFERENCES attribute_values (id)
            ON DELETE RESTRICT;