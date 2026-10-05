# Deploying HRMS (frontend + backend) on a Single EC2 Instance

Everything runs on one server:

```
Browser ──► http://<EC2_IP>  (port 80)
              │
            Nginx
              ├── /         → React build files  (/var/www/hrms)
              └── /api/...  → Spring Boot on 127.0.0.1:8080
                                    │
                                 MySQL (MariaDB) on 127.0.0.1:3306
```

The browser only talks to port 80. Spring Boot and MySQL listen on localhost only, so
ports 8080 and 3306 are **never** opened in the security group. Because the page and the
API share one origin, there are no CORS problems.

Code locations:

| Part | Git branch | Build output |
|---|---|---|
| Backend (Spring Boot, Java 17) | `HRM_Backend` | `target/backend-0.0.1-SNAPSHOT.jar` |
| Frontend (React, CRA) | `hrm_frontend` | `build/` folder |

---

## 1. Test locally first

### Option A – normal development (day-to-day)

1. Start MySQL locally (database `hrms`, user `root` / `root`).
2. Backend: on the `HRM_Backend` branch run `mvn spring-boot:run` → `http://localhost:8080`
3. Frontend: on the `hrm_frontend` branch run `npm install` then `npm start` → `http://localhost:3000`

The frontend falls back to `http://localhost:8080` and the backend allows
`http://localhost:3000` by default, so no configuration is needed.

### Option B – production-like test (same setup as EC2, before you deploy)

This runs the **built** frontend behind Nginx exactly like the server does. It needs
Docker Desktop running.

1. **Start the backend** allowing the `http://localhost` origin (PowerShell):

   ```powershell
   $env:CORS_ORIGIN="http://localhost"; $env:FRONTEND_URL="http://localhost"; mvn spring-boot:run
   ```

2. **Build the frontend** pointing at the same origin. In the frontend folder create
   `.env.production.local`:

   ```
   REACT_APP_API_URL=http://localhost
   ```

   then run `npm run build`.

3. **Create `nginx-local.conf`** in the frontend folder. It is the same as the server
   config in step 4 below, except Nginx forwards to your Windows host instead of `127.0.0.1`:

   ```nginx
   server {
       listen 80;
       root /usr/share/nginx/html;
       index index.html;
       client_max_body_size 10M;

       location /api/ {
           proxy_pass http://host.docker.internal:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }

       location / {
           try_files $uri /index.html;
       }
   }
   ```

4. **Run Nginx** from the frontend folder:

   ```powershell
   docker run --rm -p 80:80 `
     -v "${PWD}\build:/usr/share/nginx/html:ro" `
     -v "${PWD}\nginx-local.conf:/etc/nginx/conf.d/default.conf:ro" `
     nginx:alpine
   ```

5. Open **http://localhost** and log in. In DevTools → Network, API calls should go to
   `http://localhost/api/...`, not `:8080`. If this works, the EC2 deployment will work
   the same way.

> Delete `.env.production.local` (or change it to the EC2 address) before building for
> the server.

---

## 2. Launch the EC2 instance

AWS Console → EC2 → **Launch instance**

| Setting | Value |
|---|---|
| AMI | Amazon Linux 2023 |
| Type | `t3.small` (2 GB) recommended. `t2.micro`/`t3.micro` (1 GB, free tier) works with the swap file below. |
| Storage | 20 GB gp3 |
| Key pair | create/download a `.pem` |

**Security group inbound rules:**

| Port | Source | Why |
|---|---|---|
| 22 (SSH) | My IP | admin access |
| 80 (HTTP) | 0.0.0.0/0 | the app |
| 443 (HTTPS) | 0.0.0.0/0 | only if you add HTTPS later |

Do **not** open 8080 or 3306.

Then **allocate an Elastic IP** (EC2 → Elastic IPs → Allocate → Associate with the
instance). The frontend build contains the server address, so the IP must not change
when the instance restarts. Below, `<EC2_IP>` means this Elastic IP (or your domain name).

---

## 3. Prepare the server

```bash
ssh -i your-key.pem ec2-user@<EC2_IP>
```

```bash
# Java 17, Nginx, MariaDB (MySQL-compatible)
sudo dnf install -y java-17-amazon-corretto nginx mariadb105-server

# 2 GB swap – important on 1 GB instances (Java + MySQL together)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

sudo systemctl enable --now mariadb nginx
sudo mysql_secure_installation      # set a root password, answer Y to the rest
```

Create the database and an app user:

```bash
sudo mysql -u root -p
```

```sql
CREATE DATABASE hrms;
CREATE USER 'hrms_app'@'localhost' IDENTIFIED BY 'ChooseAStrongDbPassword!';
GRANT ALL PRIVILEGES ON hrms.* TO 'hrms_app'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Tables are created automatically on first start (`ddl-auto=update`).

---

## 4. Deploy

### 4.1 Build both parts on your PC

Building on the server is slow, and the React build can run out of memory on 1 GB.

**Backend** (`HRM_Backend` branch):

```powershell
# Keep the local Gmail credentials out of the jar
Move-Item src\main\resources\application-local.properties ..\application-local.properties.bak
mvn clean package -DskipTests
Move-Item ..\application-local.properties.bak src\main\resources\application-local.properties
```

**Frontend** (`hrm_frontend` branch). Create or update `.env.production.local`:

```
REACT_APP_API_URL=http://<EC2_IP>
```

No `:8080` here, because Nginx forwards `/api`. Then:

```powershell
npm install
npm run build
```

### 4.2 Copy to the server

```powershell
scp -i your-key.pem target\backend-0.0.1-SNAPSHOT.jar ec2-user@<EC2_IP>:/home/ec2-user/app.jar
scp -i your-key.pem -r build ec2-user@<EC2_IP>:/home/ec2-user/build
```

On the server:

```bash
sudo mkdir -p /opt/hrms /var/www/hrms
sudo mv ~/app.jar /opt/hrms/app.jar
sudo rm -rf /var/www/hrms/* && sudo cp -r ~/build/* /var/www/hrms/ && rm -rf ~/build
```

### 4.3 Backend configuration (secrets live only on the server)

```bash
sudo nano /opt/hrms/hrms.env
```

```ini
SERVER_ADDRESS=127.0.0.1
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hrms
SPRING_DATASOURCE_USERNAME=hrms_app
SPRING_DATASOURCE_PASSWORD=ChooseAStrongDbPassword!
SPRING_JPA_SHOWSQL=false

JWT_SECRET=paste-a-long-random-string-here-at-least-64-characters-long
CORS_ORIGIN=http://<EC2_IP>
FRONTEND_URL=http://<EC2_IP>

ADMIN_SEED_EMAIL=admin@yourcompany.com
ADMIN_SEED_PASSWORD=ChangeThisAdminPassword!
SUPER_ADMIN_SEED_EMAIL=superadmin@yourcompany.com
SUPER_ADMIN_SEED_PASSWORD=ChangeThisSuperAdminPassword!

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-account@gmail.com
SMTP_PASSWORD=your-gmail-app-password
MAIL_FROM=your-account@gmail.com
```

```bash
sudo chmod 600 /opt/hrms/hrms.env
```

Generate a JWT secret with `openssl rand -base64 64 | tr -d '\n'`.

> Login sends an OTP by email, so SMTP must work or nobody can log in.

### 4.4 Run the backend as a service

```bash
sudo nano /etc/systemd/system/hrms-backend.service
```

```ini
[Unit]
Description=HRMS Backend
After=network.target mariadb.service
Requires=mariadb.service

[Service]
User=ec2-user
EnvironmentFile=/opt/hrms/hrms.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/hrms/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now hrms-backend
sudo journalctl -u hrms-backend -f      # wait for "Started EmployeeManagementApplication", then Ctrl+C
```

### 4.5 Nginx

```bash
sudo nano /etc/nginx/conf.d/hrms.conf
```

```nginx
server {
    listen 80 default_server;
    server_name _;
    root /var/www/hrms;
    index index.html;
    client_max_body_size 10M;          # employee documents, logos, signatures

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;   # required – Spring uses it to treat requests as same-origin
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;       # report / Excel exports
    }

    location / {
        try_files $uri /index.html;    # React Router: deep links like /admin/tickets
    }
}
```

Amazon Linux's default `nginx.conf` may also mark its own server block `default_server`. If so, remove that
flag so ours wins (the `sed` below does nothing if the flag is not there):

```bash
sudo sed -i 's/listen       80 default_server;/listen       80;/; s/listen       \[::\]:80 default_server;/listen       [::]:80;/' /etc/nginx/nginx.conf
sudo chmod 755 /var/www /var/www/hrms
sudo nginx -t && sudo systemctl reload nginx
```

---

## 5. Verify

On the server:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/holidays   # 401/403 = backend up
curl -s http://localhost/ | head -5                                          # React index.html
curl -s -o /dev/null -w "%{http_code}\n" http://localhost/api/holidays        # same code as above, via Nginx
```

In the browser:

1. Open `http://<EC2_IP>`. The login page should load.
2. Log in with the admin seed account. Check that the OTP email arrives.
3. In DevTools → Network, API calls should go to `http://<EC2_IP>/api/...` and return 200.
4. Refresh on a deep page (e.g. `/admin/tickets`). It should not show a 404.

---

## 6. Redeploying

| Changed | Steps |
|---|---|
| Backend | build jar → `scp` → `sudo mv ~/app.jar /opt/hrms/app.jar && sudo systemctl restart hrms-backend` |
| Frontend | `npm run build` → `scp -r build` → `sudo rm -rf /var/www/hrms/* && sudo cp -r ~/build/* /var/www/hrms/` (no restart needed) |
| Config | edit `/opt/hrms/hrms.env` → `sudo systemctl restart hrms-backend` |

---

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| **502 Bad Gateway** on `/api` | Backend is not running. Check `sudo journalctl -u hrms-backend -n 100`. |
| **403 "Invalid CORS request"** | `CORS_ORIGIN` must exactly match the address in the browser bar (`http://` + IP or domain, no trailing slash, no port). Also check that `proxy_set_header Host $host;` is present. |
| Calls go to `:8080` or `localhost` | The frontend was built without the right `REACT_APP_API_URL`. Fix `.env.production.local` and rebuild. |
| **413** on file upload | Raise `client_max_body_size` in Nginx. |
| Backend killed / very slow | Out of memory. Confirm swap is on (`free -h`) or use `t3.small`. |
| 404 on page refresh | `try_files $uri /index.html;` is missing. |

## 8. Optional: domain + HTTPS

Point a domain's A record at the Elastic IP, then:

```bash
sudo dnf install -y certbot python3-certbot-nginx
sudo certbot --nginx -d hrms.yourdomain.com
```

After that, change `CORS_ORIGIN`/`FRONTEND_URL` to `https://hrms.yourdomain.com`, rebuild
the frontend with `REACT_APP_API_URL=https://hrms.yourdomain.com`, and redeploy both.
