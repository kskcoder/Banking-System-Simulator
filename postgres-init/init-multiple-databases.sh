#!/bin/bash
set -e

POSTGRES_AUTH_DB=$(grep "^POSTGRES_DB=" /tmp/bankauthservice.env 2>/dev/null | cut -d'=' -f2 || echo "bankauthdb")
POSTGRES_ACCOUNT_DB=$(grep "^POSTGRES_DB=" /tmp/bankaccountservice.env 2>/dev/null | cut -d'=' -f2 || echo "bankaccountdb")
POSTGRES_TRANSACTION_DB=$(grep "^POSTGRES_DB=" /tmp/banktransactionservice.env 2>/dev/null | cut -d'=' -f2 || echo "banktransactiondb")
POSTGRES_PAYMENT_DB=$(grep "^POSTGRES_DB=" /tmp/bankpaymentservice.env 2>/dev/null | cut -d'=' -f2 || echo "bankpaymentdb")
POSTGRES_CARD_DB=$(grep "^POSTGRES_DB=" /tmp/bankcardservice.env 2>/dev/null | cut -d'=' -f2 || echo "")

create_db_if_not_exists() {
    local dbname=$1
    if [ -n "$dbname" ]; then
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -tc "SELECT 1 FROM pg_database WHERE datname = '$dbname'" | grep -q 1 || psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "CREATE DATABASE \"$dbname\""
    fi
}

create_db_if_not_exists "$POSTGRES_AUTH_DB"
create_db_if_not_exists "$POSTGRES_ACCOUNT_DB"
create_db_if_not_exists "$POSTGRES_TRANSACTION_DB"
create_db_if_not_exists "$POSTGRES_PAYMENT_DB"
create_db_if_not_exists "$POSTGRES_CARD_DB"
