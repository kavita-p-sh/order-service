DROP DATABASE IF EXISTS order_service_db;
CREATE DATABASE order_service_db;
USE order_service_db;

CREATE TABLE IF NOT EXISTS tb_order_status (
    status_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS tb_orders (
    order_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    status_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    total_quantity INT NOT NULL,

    created_by VARCHAR(50),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_status
        FOREIGN KEY (status_id) REFERENCES tb_order_status(status_id)
);

CREATE TABLE IF NOT EXISTS tb_order_items (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity INT NOT NULL,

    CONSTRAINT fk_order_items_orders
        FOREIGN KEY (order_id) REFERENCES tb_orders(order_id)
);

CREATE TABLE IF NOT EXISTS tb_stock_reduce (
    stock_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL,

    created_by VARCHAR(50),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_reduce_orders
        FOREIGN KEY (order_id) REFERENCES tb_orders(order_id)
);