CREATE TABLE `users`
(
    `uuid`       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `username`   VARCHAR(50)  NOT NULL,
    `email`      VARCHAR(255) NOT NULL,
    `password`   VARCHAR(255) NOT NULL,
    `nickname`   VARCHAR(50)  NOT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME              NULL,
    UNIQUE KEY `uq_users_username` (`username`),
    UNIQUE KEY `uq_users_email` (`email`)
) ENGINE = InnoDB
  CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `categories`
(
    `uuid`        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `code`        VARCHAR(20) NOT NULL,
    `code_number` VARCHAR(3)  NOT NULL,
    `name`        VARCHAR(30) NOT NULL,
    `type`        TINYINT(1)  NOT NULL,
    UNIQUE KEY `uq_categories_code_type` (`code`, `type`),
    UNIQUE KEY `uq_categories_code_number` (`code_number`)
) ENGINE = InnoDB
  CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `transactions`
(
    `uuid`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `uuid_user`        BIGINT UNSIGNED NOT NULL,
    `uuid_category`    BIGINT UNSIGNED NOT NULL,
    `type`             TINYINT(1)      NOT NULL,
    `amount`           DECIMAL(12, 0)  NOT NULL CHECK (`amount` > 0),
    `memo`             VARCHAR(255)             NULL,
    `transaction_date` DATE            NOT NULL,
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       DATETIME                 NULL,
    CONSTRAINT `fk_transactions_user` FOREIGN KEY (`uuid_user`) REFERENCES `users` (`uuid`),
    CONSTRAINT `fk_transactions_category` FOREIGN KEY (`uuid_category`) REFERENCES `categories` (`uuid`),
    INDEX `idx_transactions_user_date` (`uuid_user`, `transaction_date`),
    INDEX `idx_transactions_user_category` (`uuid_user`, `uuid_category`)
) ENGINE = InnoDB
  CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `recurring_items`
(
    `uuid`          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    `uuid_user`     BIGINT UNSIGNED  NOT NULL,
    `uuid_category` BIGINT UNSIGNED  NOT NULL,
    `type`          TINYINT(1)       NOT NULL,
    `name`          VARCHAR(100)     NOT NULL,
    `amount`        DECIMAL(12, 0)   NOT NULL CHECK (`amount` > 0),
    `billing_day`   TINYINT UNSIGNED NOT NULL CHECK (`billing_day` BETWEEN 1 AND 31),
    `is_active`     TINYINT(1)       NOT NULL DEFAULT 1,
    `created_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    DATETIME                  NULL,
    CONSTRAINT `fk_recurring_items_user` FOREIGN KEY (`uuid_user`) REFERENCES `users` (`uuid`),
    CONSTRAINT `fk_recurring_items_category` FOREIGN KEY (`uuid_category`) REFERENCES `categories` (`uuid`)
) ENGINE = InnoDB
  CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `budgets`
(
    `uuid`       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `uuid_user`  BIGINT UNSIGNED NOT NULL,
    `year_month` CHAR(7)         NOT NULL,
    `amount`     DECIMAL(12, 0)  NOT NULL CHECK (`amount` > 0),
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_budgets_user` FOREIGN KEY (`uuid_user`) REFERENCES `users` (`uuid`),
    UNIQUE KEY `uq_budgets_user_year_month` (`uuid_user`, `year_month`)
) ENGINE = InnoDB
  CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
