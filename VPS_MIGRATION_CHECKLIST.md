# ✅ VPS Migration Checklist

Copy this checklist and check off each step as you complete it.

---

## 🖥️ ON YOUR VPS (via SSH)

### Phase 1: Database
- [ ] Install MySQL: `sudo apt install mysql-server`
- [ ] Run: `sudo mysql_secure_installation`
- [ ] Create database: `uhabits_analytics`
- [ ] Create user: `uhabits_user`
- [ ] Create 3 tables: `habit_syncs`, `analytics_summary`, `habit_performance`
- [ ] Test connection: `mysql -u uhabits_user -p`
- [ ] **SAVE** database password somewhere safe

### Phase 2: API Setup
- [ ] Generate API key: `openssl rand -base64 32 | tr -d "=+/" | cut -c1-32`
- [ ] **SAVE** API key somewhere safe
- [ ] Create directory: `/var/www/your-domain.com/api`
- [ ] Create file: `/var/www/your-domain.com/api/sync.php`
- [ ] Update `sync.php` with your database password
- [ ] Update `sync.php` with your API key
- [ ] Create file: `/var/www/your-domain.com/api/groups.php`
- [ ] Update `groups.php` with your database password
- [ ] Update `groups.php` with your API key
- [ ] Set permissions: `sudo chown -R $USER:www-data /var/www/your-domain.com`

### Phase 3: Nginx
- [ ] Create config: `/etc/nginx/sites-available/uhabits-api`
- [ ] Update config with your domain name
- [ ] Enable site: `sudo ln -s /etc/nginx/sites-available/uhabits-api /etc/nginx/sites-enabled/`
- [ ] Test config: `sudo nginx -t`
- [ ] Reload Nginx: `sudo systemctl reload nginx`

### Phase 4: SSL Certificate
- [ ] Install Certbot: `sudo apt install certbot python3-certbot-nginx`
- [ ] Get certificate: `sudo certbot --nginx -d your-domain.com`
- [ ] Choose redirect HTTP to HTTPS: Option 2
- [ ] Test auto-renewal: `sudo certbot renew --dry-run`

### Phase 5: Testing
- [ ] Test health check: `curl -H "x-api-key: YOUR_KEY" https://your-domain.com/api/sync.php`
- [ ] Verify JSON response with `"status": "healthy"`
- [ ] Test sync endpoint with POST request
- [ ] Check database: `SELECT COUNT(*) FROM uhabits_analytics.habit_syncs;`
- [ ] Verify test data appears in database

### Phase 6: Config Files
- [ ] Create: `/var/www/your-domain.com/public/cloud_config.json`
- [ ] Update with your API key
- [ ] Download to laptop: `scp user@vps:/var/www/.../cloud_config.json ~/Downloads/`

### Phase 7: Monitoring
- [ ] Create health check script
- [ ] Test script works
- [ ] Bookmark URLs for checking

---

## 💻 ON YOUR LAPTOP

### Phase 8: Update Android App
- [ ] Replace `uhabits-android/src/main/assets/cloud_config.json`
- [ ] Edit `CloudSyncManager.kt` - replace AWS URLs (lines 93, 199)
- [ ] Update default baseUrl to your domain

### Phase 9: Update Dashboards
- [ ] Update `enhanced_dashboard.html` - replace AWS URLs (lines 543, 643, 667)
- [ ] Update `group_analytics_dashboard.html` - replace AWS URLs (lines 1337, 1519)
- [ ] Or use sed commands from the manual guide

### Phase 10: Rebuild & Test
- [ ] Clean build: `./gradlew clean`
- [ ] Build debug APK: `./gradlew :uhabits-android:assembleDebug`
- [ ] Install APK on phone
- [ ] Enable cloud sync in app settings
- [ ] Trigger manual sync
- [ ] Check VPS database for new data
- [ ] Open dashboard and verify data displays

---

## 📝 Information to Save

Write these down in a safe place:

```
VPS DETAILS:
  Domain: ___________________________
  IP Address: ________________________

DATABASE:
  Name: uhabits_analytics
  User: uhabits_user
  Password: __________________________

API:
  Key: _______________________________
  Endpoint: https://[domain]/api/sync.php
  Groups: https://[domain]/api/groups.php

SSL:
  Issued: ___________________________
  Expires: __________________________
  Auto-renew: ✓
```

---

## 🧪 Final Verification

After everything is complete:

- [ ] ✅ API health check returns 200 OK
- [ ] ✅ Android app connects and syncs
- [ ] ✅ Data appears in VPS database
- [ ] ✅ Dashboard displays real data from VPS
- [ ] ✅ SSL certificate is valid (green padlock)
- [ ] ✅ No AWS services being used
- [ ] ✅ All passwords and keys saved securely

---

## ⏱️ Time Estimates

- **VPS Setup (Steps 1-7):** 30-45 minutes
- **Laptop Updates (Steps 8-9):** 10-15 minutes  
- **Testing (Step 10):** 10-15 minutes
- **Total:** ~1 hour

---

## 🆘 Emergency Contacts

If something goes wrong:

**Check Logs:**
```bash
# API errors
tail -f /var/www/your-domain.com/logs/error.log

# PHP errors  
sudo tail -f /var/log/php8.2-fpm.log

# Nginx errors
sudo tail -f /var/log/nginx/error.log

# Database
mysql -u uhabits_user -p
> SHOW TABLES;
```

**Restart Services:**
```bash
sudo systemctl restart nginx
sudo systemctl restart php8.2-fpm
sudo systemctl restart mysql
```

**Test Components:**
```bash
# Database
mysql -u uhabits_user -p -e "SELECT 1"

# Nginx
sudo nginx -t

# PHP
php -v

# SSL
curl -I https://your-domain.com
```

---

## 📚 Reference Files

- **Full Manual:** `VPS_MANUAL_SETUP_STEPS.md`
- **Migration Guide:** `VPS_MIGRATION_GUIDE.md`  
- **Quick Start:** `VPS_MIGRATION_QUICK_START.md`

---

## 🎯 Success Criteria

You're done when:
1. Curl test returns healthy status
2. Android app syncs successfully
3. Dashboard shows VPS data
4. Database has habit records
5. No AWS URLs remain in code

---

**Print this checklist and check items off as you go!** 📋✅
