# Tenant setting initialization

Every key in `app.sewain.settings.SettingKey` must have a default registration in a tenant Flyway migration before application features read it. New keys need a new migration; do not modify migrations already applied to existing databases.

Tenant migrations live under `backend/src/main/resources/db/tenant`, matching `MerchantSchemaService`'s `classpath:db/tenant` location. Do not move them to a different directory without also planning Flyway compatibility. Public migrations use the separate `db/migration` directory.

`V37__initialize_all_settings.sql` registers all current keys. It fills missing rows only through `ON CONFLICT (setting_key) DO NOTHING`, preserving existing merchant configuration, including payment choices and bank accounts. V10 and V35 remain unchanged to preserve existing Flyway checksums.

| Key | Default for a missing row |
| --- | --- |
| REGIONAL | Indonesia/IDR, Asia/Jakarta, Indonesian locale, DD/MM/YYYY, Monday week start |
| RENTAL_PLAN | One DAY/1 template at 3 percent |
| PAYMENT | Cash at pickup enabled, bank transfer/cash at return disabled; 30-percent down payment and full payment choices enabled; empty instructions |
| PAYMENT_BANK_ACCOUNTS | Empty accounts list; no fabricated bank details |
| RENTAL | Two-hour notice, 90-day advance window, no buffer, Monday–Saturday 09:00–18:00; late-return fees enabled with a one-hour grace period |

Fresh merchants receive these defaults during provisioning before READY. Existing merchants receive missing rows when tenant migrations run. Root Docker Compose enables this through `MIGRATE_TENANTS=true`; native startup uses `APP_MAINTENANCE_MIGRATE_TENANTS=true`. Public Flyway migrations alone do not migrate merchant schemas.

For existing settings, this migration does not merge fields, fix invalid/null values, or enable payment methods. If an existing merchant has bank transfer enabled without an account, update that merchant's payment configuration separately. Persisted settings take precedence over frontend form fallbacks.

## Deployment and manual verification

No builds, migrations or tests were run while adding this migration. When ready, rebuild and recreate the backend with tenant migrations enabled. For root Compose:

```sh
docker compose build backend
docker compose up -d --no-deps backend
docker compose logs -f --tail=100 backend
```

After startup, verify migration success for each tenant in the logs. In a database client, replace `your_tenant_schema` below with the actual schema and check all five rows:

```sql
SELECT setting_key, setting_value
FROM your_tenant_schema.settings
WHERE setting_key IN ('REGIONAL', 'RENTAL_PLAN', 'PAYMENT', 'PAYMENT_BANK_ACCOUNTS', 'RENTAL')
ORDER BY setting_key;
```

Verify both a newly provisioned tenant and an existing tenant with missing rows. Compare previously saved values before/after migration to confirm they were not overwritten, then retry order creation. This prevents missing-setting 404 responses; unrelated validation errors such as unavailable stock or closed operating days still apply.
