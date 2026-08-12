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

<<<<<<< HEAD

-- MARKETER REFERRAL SYSTEM (run once only if users.role is a MySQL ENUM)
-- If users.role is already VARCHAR, this statement is not required.
ALTER TABLE users MODIFY COLUMN role ENUM(
  'CUSTOMER','SELLER','KITCHEN','MARKETER','RIDER',
  'LOGISTICS_ADMIN','ACCOUNT_OFFICER','ADMIN'
) NOT NULL DEFAULT 'CUSTOMER';
=======
-- Marketer referral system
ALTER TABLE users
  MODIFY COLUMN role ENUM('CUSTOMER','SELLER','KITCHEN','RIDER','LOGISTICS_ADMIN','ACCOUNT_OFFICER','MARKETER','ADMIN')
  NOT NULL DEFAULT 'CUSTOMER';

-- Hibernate creates referral_code/referrals/wallet tables when ddl-auto=update.
-- These settings are controlled with Railway variables:
-- REFERRAL_NORMAL_REWARD=200
-- REFERRAL_MARKETER_REWARD=2000
-- REFERRAL_MINIMUM_WITHDRAWAL_REFERRALS=20
>>>>>>> 1f72347 (Update Oyuki backend)
