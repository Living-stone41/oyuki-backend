ALTER TABLE customer_addresses ADD COLUMN latitude DECIMAL(10,7) NULL, ADD COLUMN longitude DECIMAL(10,7) NULL;
ALTER TABLE seller_profiles ADD COLUMN business_document_url VARCHAR(500) NULL, ADD COLUMN cac_document_url VARCHAR(500) NULL;
CREATE TABLE IF NOT EXISTS kitchen_images (
 id BIGINT NOT NULL AUTO_INCREMENT,
 kitchen_profile_id BIGINT NOT NULL,
 image_url VARCHAR(500) NOT NULL,
 caption VARCHAR(200) NULL,
 display_order INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY (id),
 INDEX idx_kitchen_images_profile (kitchen_profile_id),
 CONSTRAINT fk_kitchen_image_profile FOREIGN KEY (kitchen_profile_id) REFERENCES kitchen_profiles(id) ON DELETE CASCADE
);
