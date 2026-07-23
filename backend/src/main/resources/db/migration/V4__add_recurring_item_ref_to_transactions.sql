ALTER TABLE `transactions`
    ADD COLUMN `uuid_recurring_item` BIGINT UNSIGNED NULL AFTER `uuid_category`,
    ADD CONSTRAINT `fk_transactions_recurring_item` FOREIGN KEY (`uuid_recurring_item`) REFERENCES `recurring_items` (`uuid`),
    ADD UNIQUE KEY `uq_transactions_recurring_item_date` (`uuid_recurring_item`, `transaction_date`);
