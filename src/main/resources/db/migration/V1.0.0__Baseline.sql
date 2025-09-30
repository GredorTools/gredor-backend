CREATE TABLE GREDOR_AUTH
(
    ID              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    TOKEN           CHAR(36)                          NOT NULL,
    PERSONAL_NUMBER CHAR(12)                          NOT NULL,
    ISSUED_AT       BIGINT                            NOT NULL,
    EXPIRES_AT      BIGINT                            NOT NULL
);

CREATE TABLE GREDOR_BANKID_ORDER
(
    ID              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    PERSONAL_NUMBER CHAR(12)                          NOT NULL,
    END_USER_IP     CHAR(45)                          NOT NULL,
    ORDER_REF       CHAR(36)                          NOT NULL,
    QR_START_TOKEN  CHAR(36)                          NOT NULL,
    QR_START_SECRET CHAR(36)                          NOT NULL,
    ORDER_TIME      BIGINT                            NOT NULL
);
