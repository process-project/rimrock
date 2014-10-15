# Self signed certificate

In production environment valid certificate signed by trusted organization should be used. 
If you don't have such certificate than you can generate self signed certificate:

```
mkdir /usr/share/ssl/certs/my-domain.pl
cd /usr/share/ssl/certs/my-domain.pl
(umask 077 && touch host.key host.cert host.info host.pem)
openssl genrsa 2048 > host.key
openssl req -new -x509 -nodes -sha1 -days 3650 -key host.key > host.cert
...[enter my-domain.pl for the Common Name]...
openssl x509 -noout -fingerprint -text < host.cert > host.info
cat host.cert host.key > host.pem
chmod 400 host.key host.pem
```