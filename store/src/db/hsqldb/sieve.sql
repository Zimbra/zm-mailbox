USE zimbra;

CREATE TABLE sieve (
   account_id             VARCHAR(127) NOT NULL,
   name                   VARCHAR(255) NOT NULL,
   value                  VARCHAR(1000) NOT NULL,

   UNIQUE INDEX i_name (name)
) ENGINE = InnoDB;