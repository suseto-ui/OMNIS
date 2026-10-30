# O.M.N.I.S. Cloud SQL SSL/TLS Certifikáty

Nahrajte sem (nebo do kořenového adresáře `/`) následující soubory stažené z Google Cloud Console:
1. `server-ca.pem`
2. `client-cert.pem`
3. `client-key.pem`

Po nahrání systém automaticky:
- Převede `client-key.pem` do formátu PKCS#8 (`client-key.pk8`) potřebného pro Android JDBC.
- Nakonfiguruje SSL parametry pro přímé a šifrované spojení do Cloud SQL PostgreSQL.
