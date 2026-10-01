# Deploying HRMS Frontend and Backend to AWS EC2 (Free Tier)

This guide deploys the two apps to **two separate EC2 instances**:

- **Server A (frontend):** Nginx serving the React (`HRMS_frontEnd`) static build.
- **Server B (backend):** Java 17 + the Spring Boot (`HRMS_backEnd`) jar + MySQL, running as a systemd service.

## Free tier note

AWS Free Tier gives **750 EC2 instance-hours/month total** (for the eligible type — `t2.micro` on older accounts, `t3.micro`/`t4g.micro` on newer ones). That covers *one* instance running 24/7, not two. Running two instances simultaneously all month uses ~1,480 hours combined, so you'll go slightly over the free allotment (roughly $8–10/month overage). To stay fully free, stop instances when not in use, or accept the small overage.

---

## 1. Launch two EC2 instances

AWS Console → EC2 → Launch Instance, twice:

| | Server A (frontend) | Server B (backend) |
|---|---|---|
| Name | `hrms-frontend` | `hrms-backend` |
| AMI | Amazon Linux 2023 | Amazon Linux 2023 |
| Type | t2.micro / t3.micro | t2.micro / t3.micro |
| Key pair | same or different | same or different |

**Security groups:**

- `hrms-frontend-sg`: SSH (22) from your IP, HTTP (80) from anywhere, HTTPS (443) from anywhere.
- `hrms-backend-sg`: SSH (22) from your IP, custom TCP 8080 from the frontend's origin (or your IP while testing). Do **not** open 3306 publicly — MySQL stays internal to Server B.

Note both instances' **public IPs** — you'll need them for configuration below.

---

## 2. Backend server (Server B)

SSH in:

```bash
ssh -i your-key.pem ec2-user@<BACKEND_PUBLIC_IP>
```

### Install Java 17

```bash
sudo dnf install -y java-17-amazon-corretto
java -version
```

### Install MySQL (MariaDB)

```bash
sudo dnf install -y mariadb105-server
sudo systemctl enable --now mariadb
sudo mysql_secure_installation   # set root password, follow prompts
```

### Create the database and app user

```bash
sudo mysql -u root -p
```

```sql
CREATE DATABASE hrms;
CREATE USER 'hrms_app'@'localhost' IDENTIFIED BY 'ChooseAStrongPassword!';
GRANT ALL PRIVILEGES ON hrms.* TO 'hrms_app'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Build the jar locally

On your dev machine, in `HRMS_backEnd`:

```bash
mvn clean package -DskipTests
```

This produces `target/*.jar`.

### Copy it to the server

```bash
scp -i your-key.pem target/backend-0.0.1-SNAPSHOT.jar ec2-user@<BACKEND_PUBLIC_IP>:/home/ec2-user/app.jar
```

### Create a production config on the server

Keeps secrets off the repo and out of the jar.

```bash
sudo mkdir -p /opt/hrms
sudo mv /home/ec2-user/app.jar /opt/hrms/app.jar
sudo nano /opt/hrms/application-local.properties
```

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hrms
spring.datasource.username=hrms_app
spring.datasource.password=ChooseAStrongPassword!

app.jwt.secret=some-long-random-production-secret
app.cors.allowed-origin=http://<FRONTEND_PUBLIC_IP>
app.frontend-url=http://<FRONTEND_PUBLIC_IP>

app.admin.seed-email=admin@yourcompany.com
app.admin.seed-password=ChangeThisAdminPassword!

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-smtp-account@gmail.com
spring.mail.password=your-gmail-app-password
app.mail.from=your-smtp-account@gmail.com
```

### Run it as a systemd service

Restarts automatically on crash or reboot.

```bash
sudo nano /etc/systemd/system/hrms-backend.service
```

```ini
[Unit]
Description=HRMS Backend
After=network.target mariadb.service

[Service]
User=ec2-user
ExecStart=/usr/bin/java -jar /opt/hrms/app.jar --spring.config.additional-location=file:/opt/hrms/application-local.properties
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now hrms-backend
sudo systemctl status hrms-backend
curl http://localhost:8080/api/leave-requests   # sanity check (expect 401/200, not connection refused)
```

---

## 3. Frontend server (Server A)

SSH in:

```bash
ssh -i your-key.pem ec2-user@<FRONTEND_PUBLIC_IP>
```

```bash
sudo dnf install -y nginx
sudo systemctl enable --now nginx
```

### Build locally, pointing at the backend's public IP

```bash
# HRMS_frontEnd/.env.production
REACT_APP_API_URL=http://<BACKEND_PUBLIC_IP>:8080
```

```bash
cd HRMS_frontEnd
npm install
npm run build
```

### Ship the build

```bash
scp -i your-key.pem -r build/* ec2-user@<FRONTEND_PUBLIC_IP>:/tmp/react-build/
```

On the frontend server:

```bash
sudo mkdir -p /var/www/hrms-frontend
sudo cp -r /tmp/react-build/* /var/www/hrms-frontend/
sudo chown -R nginx:nginx /var/www/hrms-frontend
```

### Nginx config

```bash
sudo nano /etc/nginx/conf.d/hrms-frontend.conf
```

```nginx
server {
    listen 80;
    server_name _;
    root /var/www/hrms-frontend;
    index index.html;

    location / {
        try_files $uri /index.html;
    }
}
```

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 4. Verify end to end

- `http://<FRONTEND_PUBLIC_IP>` loads the app.
- Log in / submit a leave request — check it reaches the backend (browser Network tab should show calls to `http://<BACKEND_PUBLIC_IP>:8080`).
- If calls fail with a CORS error, double check `app.cors.allowed-origin` on the backend exactly matches the frontend's origin (protocol + host, no trailing slash).

---

## 5. Recommended follow-ups

- **Elastic IPs**: allocate one for each instance and associate it, so IPs don't change if you stop/start the instances (an Elastic IP is free while attached to a running instance).
- **Domain + HTTPS**: point a domain at each Elastic IP, then run `certbot --nginx` on the frontend for HTTPS; the backend can sit behind Nginx too (reverse-proxy 8080 → 443) if you want HTTPS on the API as well.
- **Redeploys**:
  - Backend — rebuild jar, `scp` over, `sudo systemctl restart hrms-backend`.
  - Frontend — rebuild, `scp`, `cp` into `/var/www/hrms-frontend`, no reload needed for static file swaps.
- **Cost control**: stop both instances when not in use (`aws ec2 stop-instances` or via console) to stay within the 750-hour free tier pool.
