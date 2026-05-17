INSERT INTO tb_order_status (status_name) VALUES ('PLACED')
ON DUPLICATE KEY UPDATE status_name = VALUES(status_name);

INSERT INTO tb_order_status (status_name) VALUES ('CANCELLED')
ON DUPLICATE KEY UPDATE status_name = VALUES(status_name);

INSERT INTO tb_order_status (status_name) VALUES ('DELIVERED')
ON DUPLICATE KEY UPDATE status_name = VALUES(status_name);

INSERT INTO tb_order_status (status_name) VALUES ('PENDING')
ON DUPLICATE KEY UPDATE status_name = VALUES(status_name);

INSERT INTO tb_order_status (status_name) VALUES ('FAILED')
ON DUPLICATE KEY UPDATE status_name = VALUES(status_name);